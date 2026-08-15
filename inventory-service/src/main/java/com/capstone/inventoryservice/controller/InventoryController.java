package com.capstone.inventoryservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.capstone.inventoryservice.Inventory;
import com.capstone.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }


    // =====================================================
    // GET ALL INVENTORY
    // =====================================================

    @GetMapping
    public List<Inventory> getAllInventory() {

        return inventoryService.getAllInventory();
    }


    // =====================================================
    // GET INVENTORY BY ID
    // =====================================================

    @GetMapping("/{id}")
    public Inventory getInventoryById(
            @PathVariable Long id) {

        return inventoryService.getInventoryById(id);
    }


    // =====================================================
    // GET INVENTORY BY PRODUCT ID
    // =====================================================

    @GetMapping("/product/{productId}")
    public Inventory getInventoryByProductId(
            @PathVariable Long productId) {

        return inventoryService
                .getInventoryByProductId(productId);
    }


    // =====================================================
    // POST - ADD INVENTORY
    // =====================================================

    @PostMapping
    public ResponseEntity<Inventory> addInventory(
            @Valid @RequestBody Inventory inventory) {

        Inventory savedInventory =
                inventoryService.addInventory(inventory);

        return new ResponseEntity<>(
                savedInventory,
                HttpStatus.CREATED
        );
    }


    // =====================================================
    // PUT - UPDATE INVENTORY
    // =====================================================

    @PutMapping("/{id}")
    public Inventory updateInventory(
            @PathVariable Long id,
            @Valid @RequestBody Inventory inventory) {

        return inventoryService.updateInventory(
                id,
                inventory
        );
    }


    // =====================================================
    // DELETE - DELETE INVENTORY
    // =====================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteInventory(
            @PathVariable Long id) {

        inventoryService.deleteInventory(id);

        return ResponseEntity.ok(
                "Inventory deleted successfully."
        );
    }
 // =====================================================
 // RESERVE STOCK
 // =====================================================

  @PutMapping("/product/{productId}/reserve")
 public Inventory reserveStock(
         @PathVariable Long productId,
         @RequestParam int quantity) {

     return inventoryService.reserveStock(
             productId,
             quantity
     );
 }
//=====================================================
//RELEASE RESERVED STOCK
//=====================================================

@PutMapping("/product/{productId}/release")
public Inventory releaseStock(
       @PathVariable Long productId,
       @RequestParam int quantity) {

   return inventoryService.releaseStock(
           productId,
           quantity
   );
}
//=====================================================
//CONFIRM / CONSUME RESERVED STOCK
//=====================================================

@PutMapping("/product/{productId}/confirm")
public Inventory confirmStock(
     @PathVariable Long productId,
     @RequestParam int quantity) {

 return inventoryService.confirmStock(
         productId,
         quantity
 );
}
//=====================================================
//CHECK LOW STOCK
//=====================================================

@GetMapping("/product/{productId}/low-stock")
public boolean isLowStock(
     @PathVariable Long productId) {

 return inventoryService.isLowStock(productId);
}
//=====================================================
//GET ALL LOW-STOCK INVENTORY
//=====================================================

@GetMapping("/low-stock")
public List<Inventory> getLowStockInventory() {

 return inventoryService.getLowStockInventory();
}
}