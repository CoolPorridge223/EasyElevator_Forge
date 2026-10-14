package org.DJB.easyelevator.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import org.DJB.easyelevator.Easyelevator;
import org.DJB.easyelevator.logic.CargoLoad;
import org.DJB.easyelevator.logic.ElevatorController;
import org.DJB.easyelevator.logic.ElevatorParameters;
import org.DJB.easyelevator.screen.CargoScreenHandler;

import java.util.List;

/** 重载轿厢：空载 20 人；持久化货舱、货箱碰撞和动态载客上限只作用于本型号。 */
public class PowerfulCabinEntity extends AbstractCabinEntity {
    private static final EntityDataAccessor<Integer> CARGO_ITEMS = SynchedEntityData.defineId(
            PowerfulCabinEntity.class, EntityDataSerializers.INT);
    private final SimpleContainer cargo = new SimpleContainer(CargoLoad.SLOTS);

    public PowerfulCabinEntity(EntityType<?> type, Level world) {
        super(type, world, ElevatorParameters.SPEED, ElevatorParameters.HIGH_PASSENGER_NUM_LIMIT);
        cargo.addListener(inventory -> syncCargo());
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CARGO_ITEMS, 0);
    }

    public SimpleContainer cargo() { return cargo; }
    public int cargoItems() { return entityData.get(CARGO_ITEMS); }
    public int cargoCrates() { return CargoLoad.crates(cargoItems()); }

    private void syncCargo() {
        if (!level().isClientSide) {
            int count = 0;
            for (int i = 0; i < cargo.getContainerSize(); i++) count += cargo.getItem(i).getCount();
            entityData.set(CARGO_ITEMS, count);
        }
    }

    @Override public int passengerNumLimit() { return CargoLoad.passengers(cargoItems()); }
    @Override protected Item cabinItem() { return Easyelevator.POWERFUL_CABIN_ITEM.get(); }
    @Override public boolean heavyDuty() { return true; }

    /** 装卸只在开门停靠时进行；离厢、跨世界、死亡或实体移除后容器立即失效。 */
    public boolean canUseCargo(Player player) {
        return isAlive() && player.isAlive() && !player.isSpectator() && player.level() == level()
                && containsPassenger(player);
    }

    private boolean cargoInUse() {
        return level().players().stream().anyMatch(player ->
                player.containerMenu instanceof CargoScreenHandler handler
                        && handler.cabin() == this && canUseCargo(player));
    }

    @Override public void tick() {
        if (!level().isClientSide && cargoInUse()) super.doorCommand(true);
        super.tick();
    }

    @Override public boolean doorCommand(boolean open) {
        if (!open && cargoInUse()) return false;
        return super.doorCommand(open);
    }

    @Override public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && containsPassenger(player)) {
            if (!level().isClientSide) {
                if (canUseCargo(player)) {
                    player.openMenu(new SimpleMenuProvider(
                            (containerId, inventory, viewer) -> new CargoScreenHandler(containerId, inventory, this),
                            Component.translatable("screen.easyelevator.cargo")));
                } else player.displayClientMessage(Component.translatable("message.easyelevator.cargo_stopped"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    public AABB cargoBox(int index) {
        AABB box = CargoLoad.box(index);
        return localBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /** 不允许新出现的实体货箱与人/动物重叠；已存在的货箱不影响继续填装。 */
    public int cargoSlotLimit(int slot) {
        return Math.max(0, Math.min(64, cargoSpaceLimit() - cargoItems() + cargo.getItem(slot).getCount()));
    }

    public boolean cargoSpaceBlocked() {
        int limit = cargoSpaceLimit();
        return limit < CargoLoad.MAX_ITEMS && limit <= cargoItems();
    }

    private int cargoSpaceLimit() {
        for (int i = cargoCrates(); i < CargoLoad.MAX_CRATES; i++) {
            if (!level().getEntities(this, cargoBox(i), e -> e instanceof LivingEntity && !e.isSpectator()).isEmpty()) {
                return i * CargoLoad.ITEMS_PER_CRATE;
            }
        }
        return CargoLoad.MAX_ITEMS;
    }

    @Override public List<AABB> collisionBoxesStatic() {
        List<AABB> boxes = super.collisionBoxesStatic();
        for (int i = 0; i < cargoCrates(); i++) boxes.add(cargoBox(i));
        return boxes;
    }

    /** 站在货箱顶部也属于实体支撑，避免把移动中的箱顶乘客判为浮空。 */
    @Override public boolean supportsPassenger(Entity entity) {
        if (super.supportsPassenger(entity)) return true;
        if (!containsPassenger(entity)) return false;
        AABB feet = entity.getBoundingBox();
        for (int i=0; i<cargoCrates(); i++) {
            AABB box = cargoBox(i);
            if (Math.abs(entity.getY()-box.maxY) < .025 && feet.maxX > box.minX && feet.minX < box.maxX
                    && feet.maxZ > box.minZ && feet.minZ < box.maxZ) return true;
        }
        return false;
    }

    /** 1.20.1 通过公开槽位接口生成 NBT 快照，避免访问容器私有列表。 */
    private net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> cargoSnapshot(){
        var items=net.minecraft.core.NonNullList.withSize(cargo.getContainerSize(),net.minecraft.world.item.ItemStack.EMPTY);
        for(int i=0;i<items.size();i++)items.set(i,cargo.getItem(i));
        return items;
    }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.put("Cargo", ContainerHelper.saveAllItems(new CompoundTag(), cargoSnapshot()));
    }

    @Override protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        cargo.clearContent();
        var items=cargoSnapshot();
        ContainerHelper.loadAllItems(nbt.getCompound("Cargo"),items);
        for(int i=0;i<items.size();i++)cargo.setItem(i,items.get(i));
        syncCargo();
    }

    /** 回收和 /kill 掉货一次；区块卸载、停服和跨维度移除必须保留库存供保存/转移。 */
    @Override public void remove(RemovalReason reason) {
        if (!level().isClientSide && !isRemoved()
                && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            for (int i = 0; i < cargo.getContainerSize(); i++) {
                var stack = cargo.removeItemNoUpdate(i);
                if (!stack.isEmpty()) spawnAtLocation(stack);
            }
            syncCargo();
        }
        super.remove(reason);
    }
}
