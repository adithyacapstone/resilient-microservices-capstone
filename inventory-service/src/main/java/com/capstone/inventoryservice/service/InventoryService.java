package com.capstone.inventoryservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.capstone.inventoryservice.Inventory;
import com.capstone.inventoryservice.InventoryNotFoundException;
import com.capstone.inventoryservice.InsufficientStockException;
import com.capstone.inventoryservice.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(
            InventoryRepository inventoryRepository) {

        this.inventoryRepository = inventoryRepository;
    }


    // =====================================================
    // GET ALL INVENTORY
    // =====================================================

    public List<Inventory> getAllInventory() {

        return inventoryRepository.findAll();
    }


    // =====================================================
    // GET INVENTORY BY ID
    // =====================================================

    public Inventory getInventoryById(Long id) {

        return inventoryRepository
                .findById(id)
                .orElseThrow(
                        () -> new InventoryNotFoundException(
                                "Inventory with ID "
                                + id
                                + " not found"
                        )
                );
    }


    // =====================================================
    // GET INVENTORY BY PRODUCT ID
    // =====================================================

    public Inventory getInventoryByProductId(
            Long productId) {

        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(
                        () -> new InventoryNotFoundException(
                                "Inventory for Product ID "
                                + productId
                                + " not found"
                        )
                );
    }


    // =====================================================
    // ADD INVENTORY
    // =====================================================

    public Inventory addInventory(
            Inventory inventory) {

        return inventoryRepository.save(inventory);
    }


    // =====================================================
    // UPDATE INVENTORY
    // =====================================================

    public Inventory updateInventory(
            Long id,
            Inventory updatedInventory) {

        Inventory existingInventory =
                getInventoryById(id);

        existingInventory.setProductId(
                updatedInventory.getProductId()
        );

        existingInventory.setAvailableStock(
                updatedInventory.getAvailableStock()
        );

        existingInventory.setReservedStock(
                updatedInventory.getReservedStock()
        );

        existingInventory.setReorderLevel(
                updatedInventory.getReorderLevel()
        );

        return inventoryRepository.save(
                existingInventory
        );
    }


    // =====================================================
    // DELETE INVENTORY
    // =====================================================

    public void deleteInventory(Long id) {

        Inventory inventory =
                getInventoryById(id);

        inventoryRepository.delete(inventory);
    }


    // =====================================================
    // RECEIVE ADDITIONAL STOCK
    // =====================================================

    public Inventory receiveStock(
            Long productId,
            int quantity) {

        Inventory inventory =
                getInventoryByProductId(productId);

        if (quantity <= 0) {

            throw new RuntimeException(
                    "Quantity received must be greater than 0"
            );
        }

        inventory.setAvailableStock(
                inventory.getAvailableStock()
                + quantity
        );

        return inventoryRepository.save(
                inventory
        );
    }


    // =====================================================
    // RESERVE STOCK
    // =====================================================

    public Inventory reserveStock(
            Long productId,
            int quantity) {

        Inventory inventory =
                getInventoryByProductId(productId);

        if (quantity <= 0) {

            throw new InsufficientStockException(
                    "Quantity must be greater than 0"
            );
        }

        if (inventory.getAvailableStock() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient available stock"
            );
        }

        inventory.setAvailableStock(
                inventory.getAvailableStock()
                - quantity
        );

        inventory.setReservedStock(
                inventory.getReservedStock()
                + quantity
        );

        return inventoryRepository.save(
                inventory
        );
    }


    // =====================================================
    // RELEASE RESERVED STOCK
    // =====================================================

    public Inventory releaseStock(
            Long productId,
            int quantity) {

        Inventory inventory =
                getInventoryByProductId(productId);

        if (quantity <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than 0"
            );
        }

        if (inventory.getReservedStock() < quantity) {

            throw new RuntimeException(
                    "Insufficient reserved stock"
            );
        }

        inventory.setReservedStock(
                inventory.getReservedStock()
                - quantity
        );

        inventory.setAvailableStock(
                inventory.getAvailableStock()
                + quantity
        );

        return inventoryRepository.save(
                inventory
        );
    }


    // =====================================================
    // CONFIRM / CONSUME RESERVED STOCK
    // =====================================================

    public Inventory confirmStock(
            Long productId,
            int quantity) {

        Inventory inventory =
                getInventoryByProductId(productId);

        if (quantity <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than 0"
            );
        }

        if (inventory.getReservedStock() < quantity) {

            throw new RuntimeException(
                    "Insufficient reserved stock"
            );
        }

        inventory.setReservedStock(
                inventory.getReservedStock()
                - quantity
        );

        return inventoryRepository.save(
                inventory
        );
    }


    // =====================================================
    // CHECK LOW STOCK
    // =====================================================

    public boolean isLowStock(
            Long productId) {

        Inventory inventory =
                getInventoryByProductId(productId);

        return inventory.getAvailableStock()
                < inventory.getReorderLevel();
    }


    // =====================================================
    // GET ALL LOW-STOCK INVENTORY
    // =====================================================

    public List<Inventory> getLowStockInventory() {

        List<Inventory> allInventory =
                inventoryRepository.findAll();

        return allInventory.stream()
                .filter(inventory ->
                        inventory.getAvailableStock()
                        < inventory.getReorderLevel()
                )
                .toList();
    }
}