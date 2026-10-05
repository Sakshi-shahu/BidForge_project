package com.bidforge.rfq.controller;

import com.bidforge.rfq.dto.RfqDraft;
import com.bidforge.rfq.dto.RfqItemDraft;
import com.bidforge.rfq.entity.Rfq;
import com.bidforge.rfq.service.AuthService;
import com.bidforge.rfq.service.RfqService;
import com.bidforge.rfq.util.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final RfqService rfqService;
    private final CurrentUser currentUser;
    private final AuthService authService;

    public AdminController(RfqService rfqService, CurrentUser currentUser, AuthService authService) {
        this.rfqService = rfqService;
        this.currentUser = currentUser;
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        requireAdmin();
        List<Rfq> rfqs = rfqService.all();
        model.addAttribute("rfqs", rfqs);
        model.addAttribute("finalized", rfqService.finalized());
        model.addAttribute("openCount", rfqs.stream().filter(r -> "OPEN".equalsIgnoreCase(r.getStatus())).count());
        return "admin/dashboard";
    }

    @GetMapping("/rfqs")
    public String rfqs(Model model) {
        requireAdmin();
        model.addAttribute("rfqs", rfqService.all());
        return "admin/rfq-list";
    }

    @GetMapping("/vendors")
    public String vendors(Model model) {
        requireAdmin();

        List<com.bidforge.rfq.entity.User> vendors = rfqService.vendors();
        model.addAttribute("vendors", vendors);
        return "admin/vendors";
    }

    @PostMapping("/vendors")
    public String createVendor(@RequestParam String name,
                               @RequestParam String email,
                               RedirectAttributes attributes) {
        requireAdmin();
        try {
            authService.register(name, email);
            attributes.addFlashAttribute("success",
                    "Vendor created. Temporary password has been sent to the vendor's email.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vendors";
    }

    @GetMapping("/rfq/add")
    public String addRfq(Model model) {
        requireAdmin();
        RfqDraft draft = newDraft();
        prepareRfqPage(model, draft, false, null);
        return "rfq/add-rfq";
    }

    @GetMapping("/rfq/{id}/edit")
    public String editDraft(@PathVariable Integer id, Model model) {
        requireAdmin();
        Rfq rfq = rfqService.get(id);
        if (!"DRAFT".equalsIgnoreCase(rfq.getStatus())) {
            throw new IllegalArgumentException("Only draft RFQs can be edited");
        }
        RfqDraft draft = fromEntity(rfq);
        prepareRfqPage(model, draft, true, null);
        return "rfq/add-rfq";
    }

    @PostMapping("/rfq/save")
    public String saveRfq(@ModelAttribute("draft") RfqDraft draft,
                          @RequestParam(defaultValue = "DRAFT") String action,
                          Model model,
                          RedirectAttributes attributes) {
        requireAdmin();
        boolean publish = "PUBLISH".equalsIgnoreCase(action);
        try {
            if (draft.getRfqId() == null) {
                rfqService.create(draft, currentUser.get(), publish);
            } else {
                rfqService.updateDraft(draft.getRfqId(), draft, currentUser.get(), publish);
            }
            attributes.addFlashAttribute("success", publish ? "RFQ published successfully." : "RFQ saved as draft.");
            return "redirect:/admin/rfqs";
        } catch (IllegalArgumentException ex) {
            prepareRfqPage(model, draft, draft.getRfqId() != null, ex.getMessage());
            return "rfq/add-rfq";
        }
    }

    @GetMapping("/rfq/{id}")
    public String rfqDetails(@PathVariable Integer id, Model model) {
        requireAdmin();
        model.addAttribute("rfq", rfqService.get(id));
        model.addAttribute("quotations", rfqService.quotations(id));
        model.addAttribute("history", rfqService.history(id));
        return "admin/rfq-details";
    }

    @GetMapping("/quotation/{id}")
    public String quotationDetails(@PathVariable Integer id, Model model) {
        requireAdmin();
        var quotation = rfqService.quotation(id);
        model.addAttribute("quotation", quotation);
        return "admin/quotation-details";
    }

    @PostMapping("/quotation/{id}/approve")
    public String approve(@PathVariable Integer id, RedirectAttributes attributes) {
        requireAdmin();
        try {
            rfqService.approveQuotation(id, currentUser.get());
            attributes.addFlashAttribute("success", "Quotation approved and RFQ finalized.");
        } catch (IllegalArgumentException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rfqs";
    }

    @PostMapping("/quotation/{id}/reject")
    public String reject(@PathVariable Integer id, RedirectAttributes attributes) {
        requireAdmin();
        try {
            rfqService.rejectQuotation(id, currentUser.get());
            attributes.addFlashAttribute("success", "Quotation rejected.");
        } catch (IllegalArgumentException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rfqs";
    }

    @PostMapping("/rfq/{id}/close")
    public String close(@PathVariable Integer id, RedirectAttributes attributes) {
        requireAdmin();
        try {
            rfqService.close(id, currentUser.get());
            attributes.addFlashAttribute("success", "RFQ closed.");
        } catch (IllegalArgumentException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rfqs";
    }

    @GetMapping("/rfq/{id}/extend")
    public String extendPage(@PathVariable Integer id, Model model) {
        requireAdmin();
        model.addAttribute("rfq", rfqService.get(id));
        model.addAttribute("vendors", rfqService.vendors());
        model.addAttribute("assignedVendorIds", rfqService.assignedToVendors(id));
        return "admin/extend-rfq";
    }

    @PostMapping("/rfq/{id}/extend")
    public String extend(@PathVariable Integer id,
                         @RequestParam String newEndDate,
                         @RequestParam(name = "vendorIds", required = false) List<Integer> vendorIds,
                         RedirectAttributes attributes) {
        requireAdmin();
        try {
            rfqService.reopen(id, LocalDate.parse(newEndDate), vendorIds == null ? List.of() : vendorIds, currentUser.get());
            attributes.addFlashAttribute("success", "RFQ reopened with the new closing date.");
        } catch (IllegalArgumentException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rfqs";
    }

    @GetMapping("/quotations")
    public String quotations(Model model) {
        requireAdmin();
        model.addAttribute("quotations", rfqService.allQuotations());
        return "admin/quotations";
    }

    @GetMapping("/finalized")
    public String finalized(Model model) {
        requireAdmin();
        model.addAttribute("finalized", rfqService.finalized());
        return "admin/finalized";
    }

    private RfqDraft newDraft() {
        RfqDraft draft = new RfqDraft();
        draft.setRfqNo(rfqService.nextRfqNo());
        draft.setIndentNo(rfqService.nextIndentNo());
        return draft;
    }

    private RfqDraft fromEntity(Rfq rfq) {
        RfqDraft draft = new RfqDraft();
        draft.setRfqId(rfq.getRfqId());
        draft.setRfqNo(rfq.getRfqNo());
        draft.setIndentNo(rfq.getIndentNo());
        draft.setMobile(rfq.getMobile());
        draft.setContactPerson(rfq.getContactPerson());
        if (rfq.getBidDate() != null) draft.setBidStartDate(rfq.getBidDate().toLocalDate());
        if (rfq.getExpiryDateOfBid() != null) draft.setBidEndDate(rfq.getExpiryDateOfBid().toLocalDate());
        rfq.getItems().forEach(item -> {
            RfqItemDraft line = new RfqItemDraft();
            line.setLineNo(item.getRfqLineNo());
            line.setItemNo(item.getItemNo());
            line.setItemName(item.getItemName());
            line.setFactoryCode(item.getFactoryCode());
            line.setRequiredQuantity(item.getReqQty());
            line.setUom(item.getUom());
            if (item.getReqDeliveryDate() != null) line.setDeliveryDate(item.getReqDeliveryDate().toLocalDate());
            line.setDeliveryLocation(item.getDeliveryLocation());
            line.setDescription(item.getDescription());
            draft.getItems().add(line);
        });
        draft.setVendorIds(rfqService.assignedToVendors(rfq.getRfqId()));
        return draft;
    }

    private void prepareRfqPage(Model model, RfqDraft draft, boolean editMode, String error) {
        model.addAttribute("draft", draft);
        model.addAttribute("vendors", rfqService.vendors());
        model.addAttribute("editMode", editMode);
        if (error != null) model.addAttribute("error", error);
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) throw new IllegalStateException("Admin access required");
    }
}
