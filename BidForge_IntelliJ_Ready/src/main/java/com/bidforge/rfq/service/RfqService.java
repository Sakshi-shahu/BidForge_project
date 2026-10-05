package com.bidforge.rfq.service;

import com.bidforge.rfq.dto.RfqDraft;
import com.bidforge.rfq.dto.RfqItemDraft;
import com.bidforge.rfq.entity.FinalizedQuotation;
import com.bidforge.rfq.entity.QuotationItem;
import com.bidforge.rfq.entity.Rfq;
import com.bidforge.rfq.entity.RfqHistory;
import com.bidforge.rfq.entity.RfqItem;
import com.bidforge.rfq.entity.RfqQuotation;
import com.bidforge.rfq.entity.RfqVendor;
import com.bidforge.rfq.entity.User;
import com.bidforge.rfq.repository.FinalizedQuotationRepository;
import com.bidforge.rfq.repository.QuotationItemRepository;
import com.bidforge.rfq.repository.RfqHistoryRepository;
import com.bidforge.rfq.repository.RfqItemRepository;
import com.bidforge.rfq.repository.RfqQuotationRepository;
import com.bidforge.rfq.repository.RfqRepository;
import com.bidforge.rfq.repository.RfqVendorRepository;
import com.bidforge.rfq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RfqService {
    private final RfqRepository rfqRepository;
    private final RfqItemRepository itemRepository;
    private final RfqVendorRepository vendorRepository;
    private final RfqQuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final FinalizedQuotationRepository finalizedRepository;
    private final RfqHistoryRepository historyRepository;
    private final UserRepository userRepository;



    public String nextRfqNo() {
        return String.format("RFQ-%d-%03d", LocalDate.now().getYear(), rfqRepository.count() + 1);
    }

    public String nextIndentNo() {
        return String.format("IND-%d-%03d", LocalDate.now().getYear(), rfqRepository.count() + 1);
    }

    public List<Rfq> all() {
        return rfqRepository.findAllByOrderByRfqIdDesc();
    }

    @Transactional(readOnly = true)
    public Rfq get(Integer id) {
        Rfq rfq = rfqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RFQ not found"));
        rfq.getItems().size();
        return rfq;
    }

    public List<User> vendors() {
        return userRepository.findByRoleIgnoreCase("VENDOR");
    }

    public List<RfqVendor> assignedTo(Integer vendorId) {
        return vendorRepository.findByVendorUserIdOrderByRfqRfqIdDesc(vendorId);
    }

    public List<Integer> assignedToVendors(Integer rfqId) {
        return vendorRepository.findByRfqRfqId(rfqId).stream().map(a -> a.getVendor().getUserId()).toList();
    }

    public List<RfqQuotation> quotationsForVendor(Integer vendorId) {
        return quotationRepository.findByVendorUserIdOrderByQuotationIdDesc(vendorId);
    }

    public List<RfqQuotation> quotations(Integer rfqId) {
        return quotationRepository.findByRfqRfqIdOrderByQuotationIdDesc(rfqId);
    }

    public List<RfqQuotation> allQuotations() {
        return quotationRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "quotationId"));
    }

    public List<RfqHistory> history(Integer rfqId) {
        return historyRepository.findByRfqRfqIdOrderByCreatedAtDesc(rfqId);
    }

    public List<FinalizedQuotation> finalized() {
        return finalizedRepository.findAllByOrderByFinalIdDesc();
    }

    public RfqQuotation quotation(Integer id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quotation not found"));
    }

    @Transactional(readOnly = true)
    public RfqQuotation quotationForVendor(Integer rfqId, Integer vendorId) {
        return quotationRepository.findByRfqRfqIdAndVendorUserId(rfqId, vendorId).orElse(null);
    }

    @Transactional
    public Rfq create(RfqDraft draft, User admin, boolean publish) {
        validateDraft(draft, publish);

        Rfq rfq = new Rfq();
        rfq.setRfqNo(draft.getRfqNo());
        rfq.setIndentNo(draft.getIndentNo());
        rfq.setMobile(trim(draft.getMobile()));
        rfq.setContactPerson(trim(draft.getContactPerson()));
        rfq.setBidDate(toStartOfDay(draft.getBidStartDate()));
        rfq.setExpiryDateOfBid(toEndOfDay(draft.getBidEndDate()));
        rfq.setStatus(publish ? "OPEN" : "DRAFT");
        rfq.setCreatedBy(admin);

        Rfq saved = rfqRepository.save(rfq);
        replaceItems(saved, draft.getItems());
        assignVendors(saved, draft.getVendorIds());
        addHistory(saved, null, saved.getStatus(), "CREATED", admin, null, saved.getExpiryDateOfBid(),
                "RFQ created");
        return saved;
    }

    @Transactional
    public Rfq updateDraft(Integer id, RfqDraft draft, User admin, boolean publish) {
        Rfq rfq = get(id);
        if (!"DRAFT".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("Only draft RFQs can be edited");
        }
        validateDraft(draft, publish);

        String oldStatus = rfq.getStatus();
        rfq.setMobile(trim(draft.getMobile()));
        rfq.setContactPerson(trim(draft.getContactPerson()));
        rfq.setBidDate(toStartOfDay(draft.getBidStartDate()));
        rfq.setExpiryDateOfBid(toEndOfDay(draft.getBidEndDate()));
        rfq.setStatus(publish ? "OPEN" : "DRAFT");
        rfq.getItems().clear();
        rfq.setRfqLineNo(null);
        rfq.setItemNo(null);
        rfq.setItemName(null);
        rfq.setReqQty(null);
        rfq.setUom(null);
        rfq.setReqDeliveryDate(null);
        rfq.setDeliveryLocation(null);
        rfq.setDescription(null);

        Rfq saved = rfqRepository.save(rfq);
        replaceItems(saved, draft.getItems());
        vendorRepository.deleteByRfqRfqId(id);
        assignVendors(saved, draft.getVendorIds());

        String action = publish ? "PUBLISHED" : "UPDATED";
        addHistory(saved, oldStatus, saved.getStatus(), action, admin, null, saved.getExpiryDateOfBid(),
                publish ? "Draft published" : "Draft updated");
        return saved;
    }

    private void validateDraft(RfqDraft draft, boolean publish) {
        if (draft.getRfqNo() == null || draft.getRfqNo().isBlank()) {
            throw new IllegalArgumentException("RFQ number is required");
        }
        if (draft.getIndentNo() == null || draft.getIndentNo().isBlank()) {
            throw new IllegalArgumentException("Indent number is required");
        }
        if (draft.getItems() == null) {
            throw new IllegalArgumentException("RFQ items are required");
        }
        for (RfqItemDraft item : draft.getItems()) {
            validateItem(item);
        }
        if (publish) {
            if (draft.getItems().isEmpty()) {
                throw new IllegalArgumentException("Add at least one RFQ item before publishing");
            }
            if (draft.getBidStartDate() == null || draft.getBidEndDate() == null) {
                throw new IllegalArgumentException("Opening and closing dates are required");
            }
            if (!draft.getBidEndDate().isAfter(draft.getBidStartDate())) {
                throw new IllegalArgumentException("Closing date must be after opening date");
            }
            if (draft.getVendorIds() == null || draft.getVendorIds().isEmpty()) {
                throw new IllegalArgumentException("Select at least one vendor");
            }
        }
    }

    private void validateItem(RfqItemDraft item) {
        if (item == null || item.getItemName() == null || item.getItemName().isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (item.getRequiredQuantity() == null || item.getRequiredQuantity() <= 0) {
            throw new IllegalArgumentException("Item quantity must be greater than zero");
        }
        if (item.getUom() == null || item.getUom().isBlank()) {
            throw new IllegalArgumentException("UOM is required for every item");
        }
        if (item.getDeliveryDate() == null) {
            throw new IllegalArgumentException("Required delivery date is mandatory for every item");
        }
        if (item.getDeliveryLocation() == null || item.getDeliveryLocation().isBlank()) {
            throw new IllegalArgumentException("Delivery location is required for every item");
        }
    }

    private void replaceItems(Rfq rfq, List<RfqItemDraft> drafts) {
        for (int i = 0; i < drafts.size(); i++) {
            RfqItemDraft d = drafts.get(i);
            RfqItem item = new RfqItem();
            item.setRfq(rfq);
            item.setRfqLineNo(String.valueOf(i + 1));
            item.setItemNo(String.valueOf(i + 1));
            item.setFactoryCode(trim(d.getFactoryCode()));
            item.setItemName(d.getItemName().trim());
            item.setReqQty(d.getRequiredQuantity());
            item.setUom(d.getUom().trim());
            item.setReqDeliveryDate(d.getDeliveryDate().atStartOfDay());
            item.setDeliveryLocation(trim(d.getDeliveryLocation()));
            item.setDescription(trim(d.getDescription()));
            itemRepository.save(item);
            if (i == 0) {
                rfq.setRfqLineNo(item.getRfqLineNo());
                rfq.setItemNo(item.getItemNo());
                rfq.setItemName(item.getItemName());
                rfq.setReqQty(item.getReqQty());
                rfq.setUom(item.getUom());
                rfq.setReqDeliveryDate(item.getReqDeliveryDate());
                rfq.setDeliveryLocation(item.getDeliveryLocation());
                rfq.setDescription(item.getDescription());
            }
        }
        rfqRepository.save(rfq);
    }

    private void assignVendors(Rfq rfq, List<Integer> ids) {
        if (ids == null) return;
        for (Integer vendorId : ids) {
            User vendor = userRepository.findById(vendorId)
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
            if (!"VENDOR".equalsIgnoreCase(vendor.getRole())) {
                throw new IllegalArgumentException("Invalid vendor selection");
            }
            if (vendorRepository.existsByRfqRfqIdAndVendorUserId(rfq.getRfqId(), vendorId)) {
                continue;
            }
            RfqVendor assignment = new RfqVendor();
            assignment.setRfq(rfq);
            assignment.setVendor(vendor);
            vendorRepository.save(assignment);
        }
    }

    @Transactional
    public RfqQuotation saveQuotation(Integer rfqId, User vendor, List<Integer> itemIds,
                                      List<BigDecimal> unitPrices, String paymentTerms, String remarks,
                                      boolean submit) {
        Rfq rfq = get(rfqId);
        validateBidWindow(rfq, vendor);
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("Add pricing for at least one RFQ item");
        }
        if (unitPrices == null || unitPrices.size() != itemIds.size()) {
            throw new IllegalArgumentException("Invalid quotation item data");
        }

        RfqQuotation quotation = quotationRepository.findByRfqRfqIdAndVendorUserId(rfqId, vendor.getUserId())
                .orElseGet(RfqQuotation::new);
        quotation.setRfq(rfq);
        quotation.setVendor(vendor);
        quotation.setPaymentTerms(trim(paymentTerms));
        quotation.setRemarks(trim(remarks));
        quotation.setStatus(submit ? "SUBMITTED" : "DRAFT");
        if (submit) quotation.setSubmittedDate(LocalDateTime.now());

        quotation = quotationRepository.saveAndFlush(quotation);
        if (quotation.getBidNo() == null || quotation.getBidNo().isBlank()) {
            quotation.setBidNo(String.format("BID-%d-%05d", LocalDate.now().getYear(), quotation.getQuotationId()));
            quotationRepository.saveAndFlush(quotation);
        }


        quotationItemRepository.deleteByQuotationQuotationId(quotation.getQuotationId());
        quotationItemRepository.flush();
        quotation.getItems().clear();

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (int i = 0; i < itemIds.size(); i++) {
            RfqItem item = itemRepository.findById(itemIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("RFQ item not found"));
            if (!item.getRfq().getRfqId().equals(rfqId)) {
                throw new IllegalArgumentException("RFQ item does not belong to this RFQ");
            }

            BigDecimal unit = nz(unitPrices.get(i));
            BigDecimal gst = BigDecimal.TEN;
            if (unit.signum() <= 0) {
                throw new IllegalArgumentException("Unit price must be greater than zero");
            }

            BigDecimal itemTotal = unit.multiply(BigDecimal.valueOf(item.getReqQty()));
            BigDecimal gstAmount = itemTotal.multiply(gst).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal lineTotal = itemTotal.add(gstAmount).setScale(4, RoundingMode.HALF_UP);

            QuotationItem qi = new QuotationItem();
            qi.setQuotation(quotation);
            qi.setRfqItem(item);
            qi.setUnitPrice(unit);
            qi.setGstPercent(gst);
            qi.setGstAmount(gstAmount);
            qi.setLineTotal(lineTotal);
            quotation.getItems().add(qi);
            grandTotal = grandTotal.add(lineTotal);
        }
        quotation.setQuotedAmount(grandTotal.setScale(4, RoundingMode.HALF_UP));
        return quotationRepository.saveAndFlush(quotation);
    }

    @Transactional
    public void generateInvoice(Integer quotationId, User vendor) {
        RfqQuotation quotation = quotation(quotationId);
        if (!quotation.getVendor().getUserId().equals(vendor.getUserId())) {
            throw new IllegalArgumentException("Quotation access denied");
        }
        if (!"APPROVED".equalsIgnoreCase(quotation.getStatus())) {
            throw new IllegalArgumentException("Invoice can be generated only after Admin approves the quotation");
        }
        quotation.setInvoiceGenerated(true);
        quotationRepository.saveAndFlush(quotation);
    }

    private void validateBidWindow(Rfq rfq, User vendor) {
        if (!"OPEN".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("This RFQ is not open for bidding");
        }
        if (rfq.getExpiryDateOfBid() != null && rfq.getExpiryDateOfBid().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The RFQ bidding period has expired");
        }
        if (!vendorRepository.existsByRfqRfqIdAndVendorUserId(rfq.getRfqId(), vendor.getUserId())) {
            throw new IllegalArgumentException("You are not assigned to this RFQ");
        }
    }

    @Transactional
    public void approveQuotation(Integer quotationId, User admin) {
        RfqQuotation quotation = quotation(quotationId);
        Rfq rfq = quotation.getRfq();
        if (!"OPEN".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("Only an open RFQ can be approved");
        }
        if (!"SUBMITTED".equalsIgnoreCase(quotation.getStatus())) {
            throw new IllegalArgumentException("Only submitted quotations can be approved");
        }

        quotation.setStatus("APPROVED");
        quotationRepository.save(quotation);
        for (RfqQuotation other : quotationRepository.findByRfqRfqIdOrderByQuotationIdDesc(rfq.getRfqId())) {
            if (!other.getQuotationId().equals(quotationId) && "SUBMITTED".equalsIgnoreCase(other.getStatus())) {
                other.setStatus("REJECTED");
                quotationRepository.save(other);
            }
        }

        String old = rfq.getStatus();
        rfq.setStatus("APPROVED");
        rfqRepository.save(rfq);

        FinalizedQuotation finalized = new FinalizedQuotation();
        finalized.setRfq(rfq);
        finalized.setQuotation(quotation);
        finalized.setFinalizedDate(LocalDateTime.now());
        finalizedRepository.save(finalized);
        addHistory(rfq, old, "APPROVED", "APPROVED", admin, rfq.getExpiryDateOfBid(), rfq.getExpiryDateOfBid(),
                "Quotation approved");
    }

    @Transactional
    public void rejectQuotation(Integer quotationId, User admin) {
        RfqQuotation quotation = quotation(quotationId);
        Rfq rfq = quotation.getRfq();
        if ("APPROVED".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("Approved RFQ cannot be changed");
        }
        quotation.setStatus("REJECTED");
        quotationRepository.save(quotation);
        addHistory(rfq, rfq.getStatus(), rfq.getStatus(), "QUOTATION_REJECTED", admin,
                rfq.getExpiryDateOfBid(), rfq.getExpiryDateOfBid(), "Quotation rejected");
    }

    @Transactional
    public void close(Integer rfqId, User admin) {
        Rfq rfq = get(rfqId);
        if ("APPROVED".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("Approved RFQ cannot be closed");
        }
        String old = rfq.getStatus();
        rfq.setStatus("CLOSED");
        rfqRepository.save(rfq);
        addHistory(rfq, old, "CLOSED", "CLOSED", admin, rfq.getExpiryDateOfBid(), rfq.getExpiryDateOfBid(),
                "RFQ closed by Admin");
    }

    @Transactional
    public void reopen(Integer rfqId, LocalDate newEndDate, List<Integer> vendorIds, User admin) {
        Rfq rfq = get(rfqId);
        if (!"CLOSED".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("Only closed RFQs can be reopened");
        }
        if (newEndDate == null || !newEndDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Extended closing date must be in the future");
        }
        LocalDateTime oldEnd = rfq.getExpiryDateOfBid();
        rfq.setExpiryDateOfBid(newEndDate.atTime(LocalTime.MAX));
        rfq.setStatus("OPEN");
        rfqRepository.save(rfq);
        if (vendorIds != null && !vendorIds.isEmpty()) {
            assignVendors(rfq, vendorIds);
        }
        addHistory(rfq, "CLOSED", "OPEN", "REOPENED", admin, oldEnd, rfq.getExpiryDateOfBid(),
                "RFQ reopened with a new closing date");
    }

    private void addHistory(Rfq rfq, String oldStatus, String newStatus, String action, User actor,
                            LocalDateTime oldClosing, LocalDateTime newClosing, String remarks) {
        RfqHistory h = new RfqHistory();
        h.setRfq(rfq);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setAction(action);
        h.setActor(actor);
        h.setPreviousClosingDate(oldClosing);
        h.setNewClosingDate(newClosing);
        h.setRemarks(remarks);
        historyRepository.save(h);
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate date) {
        return date == null ? null : date.atTime(LocalTime.MAX);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }
}
