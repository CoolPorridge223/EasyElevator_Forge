package org.DJB.easyelevator.screen;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.network.chat.Component;
import org.DJB.easyelevator.Easyelevator;
import org.DJB.easyelevator.entity.PowerfulCabinEntity;
import org.DJB.easyelevator.logic.CargoLoad;

/** 原版容器事务提供拖放、快捷搬运、多人同步和光标物品回收。 */
public class CargoScreenHandler extends AbstractContainerMenu {
    private final PowerfulCabinEntity cabin;
    private final Container cargo;
    private final ContainerData properties;

    public CargoScreenHandler(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, new SimpleContainer(CargoLoad.SLOTS), new SimpleContainerData(1));
    }

    public CargoScreenHandler(int containerId, Inventory playerInventory, PowerfulCabinEntity cabin) {
        this(containerId, playerInventory, cabin, cabin.cargo(), new ContainerData() {
            public int get(int index) { return cabin.cargoItems(); }
            public void set(int index, int value) {}
            public int getCount() { return 1; }
        });
    }

    private CargoScreenHandler(int containerId, Inventory playerInventory, PowerfulCabinEntity cabin,
                               Container cargo, ContainerData properties) {
        super(Easyelevator.CARGO_SCREEN.get(), containerId);
        this.cabin = cabin;
        this.cargo = cargo;
        this.properties = properties;
        addDataSlots(properties);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            final int index = column + row * 9;
            addSlot(new Slot(cargo, index, 8 + column * 18, 54 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return getMaxStackSize(stack) > 0;
                }
                @Override public int getMaxStackSize() {
                    return cabin == null ? 64 : cabin.cargoSlotLimit(index);
                }
                @Override public int getMaxStackSize(ItemStack stack) {
                    return Math.min(stack.getMaxStackSize(), getMaxStackSize());
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 128 + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 186));
    }

    public PowerfulCabinEntity cabin() { return cabin; }
    public int cargoItems() { return properties.get(0); }
    public int passengerLimit() { return CargoLoad.passengers(cargoItems()); }

    @Override public boolean stillValid(Player player) { return cabin == null || cabin.canUseCargo(player); }

    @Override public void clicked(int slotIndex, int button, ClickType action, Player player) {
        if (!stillValid(player)) return;
        int before = cabin == null ? 0 : cabin.cargoItems();
        super.clicked(slotIndex, button, action, player);
        cargo.setChanged();
        if (cabin != null && cabin.cargoItems() == before && cabin.cargoSpaceBlocked())
            player.displayClientMessage(Component.translatable("message.easyelevator.cargo_space"), true);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < CargoLoad.SLOTS) {
            if (!moveItemStackTo(stack, CargoLoad.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Refresh the weight before considering the next slot's crate collision guard.
            boolean moved = false;
            for (int i = 0; i < CargoLoad.SLOTS && !stack.isEmpty(); i++) {
                moved |= moveItemStackTo(stack, i, i + 1, false);
                cargo.setChanged();
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        cargo.setChanged();
        return original;
    }
}
