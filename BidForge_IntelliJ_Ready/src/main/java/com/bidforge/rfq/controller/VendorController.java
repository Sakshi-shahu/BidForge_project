package com.bidforge.rfq.controller;

import com.bidforge.rfq.entity.RfqQuotation;
import com.bidforge.rfq.service.RfqService;
import com.bidforge.rfq.util.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorController {
    private final RfqService rfqService;
    private final CurrentUser currentUser;

    public VendorController(RfqService rfqService, CurrentUser currentUser) {
        this.rfqService = rfqService;
        this.currentUser = currentUser;
    }

    @GetMapping("/dashboard") public String dashboard(Model model){requireVendor();model.addAttribute("assignments",rfqService.assignedTo(currentUser.get().getUserId()));return "vendor/dashboard";}

    @GetMapping("/rfq/{id}") public String viewRfq(@PathVariable Integer id, Model model){
        requireVendor(); var rfq=rfqService.get(id); boolean assigned=rfqService.assignedTo(currentUser.get().getUserId()).stream().anyMatch(a->a.getRfq().getRfqId().equals(id));
        RfqQuotation existingQuotation = rfqService.quotationForVendor(id, currentUser.get().getUserId());
        java.util.Map<Integer, BigDecimal> existingPrices = new java.util.HashMap<>();
        if (existingQuotation != null) {
            existingQuotation.getItems().forEach(item -> existingPrices.put(item.getRfqItem().getItemId(), item.getUnitPrice()));
        }
        boolean canEdit = assigned && "OPEN".equalsIgnoreCase(rfq.getStatus())
                && (existingQuotation == null || !"APPROVED".equalsIgnoreCase(existingQuotation.getStatus()));
        model.addAttribute("rfq",rfq);
        model.addAttribute("assigned",assigned);
        model.addAttribute("quotation",existingQuotation);
        model.addAttribute("existingPrices",existingPrices);
        model.addAttribute("canEdit",canEdit);
        return "vendor/rfq-details";
    }

    @PostMapping("/rfq/{id}/quote")
    public String quote(@PathVariable Integer id, @RequestParam List<Integer> itemIds,
                        @RequestParam List<BigDecimal> unitPrices, @RequestParam String paymentTerms,
                        @RequestParam(required=false,defaultValue="") String remarks,
                        @RequestParam(defaultValue="true") boolean submit, RedirectAttributes attributes){
        requireVendor();
        try { rfqService.saveQuotation(id,currentUser.get(),itemIds,unitPrices,paymentTerms,remarks,submit); attributes.addFlashAttribute("success",submit?"Quotation submitted successfully.":"Quotation saved as draft."); }
        catch(Exception ex){attributes.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/vendor/rfq/"+id;
    }

    @GetMapping("/rfq/{id}/history")
    public String history(@PathVariable Integer id, Model model) {
        requireVendor();
        boolean assigned = rfqService.assignedTo(currentUser.get().getUserId()).stream().anyMatch(a -> a.getRfq().getRfqId().equals(id));
        if (!assigned) throw new IllegalStateException("RFQ history access denied");
        model.addAttribute("rfq", rfqService.get(id));
        model.addAttribute("history", rfqService.history(id));
        return "vendor/rfq-history";
    }

    @GetMapping("/quotations") public String submitted(Model model){requireVendor();model.addAttribute("quotations",rfqService.quotationsForVendor(currentUser.get().getUserId()));
        return "vendor/submitted-quotations";}
    @GetMapping("/quotations/{id}") public String quotation(@PathVariable Integer id, Model model){requireVendor();RfqQuotation q=rfqService.quotation(id);if(!q.getVendor().getUserId().equals(currentUser.get().getUserId()))throw new IllegalStateException("Quotation access denied");model.addAttribute("quotation",q);return "vendor/submission-details";}

    @PostMapping("/quotations/{id}/generate-invoice")
    public String generateInvoice(@PathVariable Integer id, RedirectAttributes attributes) {
        requireVendor();
        try {
            rfqService.generateInvoice(id, currentUser.get());
            attributes.addFlashAttribute("success", "Invoice marked as Generated successfully.");
        } catch (IllegalArgumentException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/vendor/quotations/" + id;
    }
    private void requireVendor(){if(!"VENDOR".equalsIgnoreCase(currentUser.get().getRole()))throw new IllegalStateException("Vendor access required");}
}
