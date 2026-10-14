package org.DJB.easyelevator;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.DJB.easyelevator.block.*;
import org.DJB.easyelevator.entity.*;
import org.DJB.easyelevator.item.CabinItem;
import org.DJB.easyelevator.network.ElevatorNetworking;
import org.DJB.easyelevator.screen.CargoScreenHandler;

/**
 * Forge 模组入口。静态阶段只登记工厂，不构造任何会创建 intrusive holder 的游戏对象。
 * Forge 在 RegisterEvent 打开对应注册表后调用工厂，避免构造阶段的 Registry is already frozen。
 * 依赖顺序为方块 → 物品/方块实体，实体类型与菜单在各自事件中登记；业务调用通过 RegistryObject.get() 取值。
 */
@Mod(Easyelevator.MOD_ID)
public class Easyelevator {
    /** 全部资源和存档 ID 保持不变。 */
    public static final String MOD_ID = "easyelevator";
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MOD_ID);

    /** 楼层门沿用 call_button 注册 ID；仅工厂执行时构造方块。 */
    public static final RegistryObject<Block> RAIL=BLOCKS.register("elevator_rail",()->new ElevatorRailBlock(BlockBehaviour.Properties.of().strength(3).noOcclusion()));
    public static final RegistryObject<Block> LANDING_DOOR=BLOCKS.register("call_button",()->new LandingDoorBlock(BlockBehaviour.Properties.of().strength(3).noOcclusion().dynamicShape()));
    public static final RegistryObject<BlockEntityType<LandingDoorBlockEntity>> LANDING_DOOR_BE=BLOCK_ENTITIES.register("landing_door",()->BlockEntityType.Builder.of(LandingDoorBlockEntity::new,LANDING_DOOR.get()).build(null));
    public static final RegistryObject<MenuType<CargoScreenHandler>> CARGO_SCREEN=MENUS.register("cargo",()->new MenuType<>(CargoScreenHandler::new,FeatureFlags.VANILLA_SET));

    /** 四类轿厢尺寸和跟踪频率相同；实体类型构造推迟至实体注册事件。 */
    public static final RegistryObject<EntityType<CabinEntity>> CABIN=ENTITIES.register("cabin",()->cabinType("cabin",CabinEntity::new));
    public static final RegistryObject<EntityType<HighSpeedCabinEntity>> HIGH_SPEED_CABIN=ENTITIES.register("high_speed_cabin",()->cabinType("high_speed_cabin",HighSpeedCabinEntity::new));
    public static final RegistryObject<EntityType<ObservationCabinEntity>> OBSERVATION_CABIN=ENTITIES.register("observation_cabin",()->cabinType("observation_cabin",ObservationCabinEntity::new));
    public static final RegistryObject<EntityType<PowerfulCabinEntity>> POWERFUL_CABIN=ENTITIES.register("powerful_cabin",()->cabinType("powerful_cabin",PowerfulCabinEntity::new));

    /** 实体类型 Supplier 延迟到放置物品时读取，不依赖物品与实体注册事件的先后顺序。 */
    public static final RegistryObject<Item> CABIN_ITEM=ITEMS.register("cabin",()->new CabinItem(new Item.Properties().stacksTo(1),CABIN::get));
    public static final RegistryObject<Item> HIGH_SPEED_CABIN_ITEM=ITEMS.register("high_speed_cabin",()->new CabinItem(new Item.Properties().stacksTo(1),HIGH_SPEED_CABIN::get));
    public static final RegistryObject<Item> OBSERVATION_CABIN_ITEM=ITEMS.register("observation_cabin",()->new CabinItem(new Item.Properties().stacksTo(1),OBSERVATION_CABIN::get));
    public static final RegistryObject<Item> POWERFUL_CABIN_ITEM=ITEMS.register("powerful_cabin",()->new CabinItem(new Item.Properties().stacksTo(1),POWERFUL_CABIN::get));
    public static final RegistryObject<Item> RAIL_ITEM=ITEMS.register("elevator_rail",()->new BlockItem(RAIL.get(),new Item.Properties()));
    public static final RegistryObject<Item> LANDING_DOOR_ITEM=ITEMS.register("call_button",()->new BlockItem(LANDING_DOOR.get(),new Item.Properties()));
    public static final RegistryObject<SoundEvent> RUNNING=SOUNDS.register("elevator_running",()->SoundEvent.createVariableRangeEvent(id("elevator_running")));
    public static final RegistryObject<SoundEvent> ARRIVAL=SOUNDS.register("elevator_arrival",()->SoundEvent.createVariableRangeEvent(id("elevator_arrival")));
    public static final RegistryObject<CreativeModeTab> TAB=TABS.register("main",()->CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.easyelevator"))
            .icon(()->new ItemStack(CABIN_ITEM.get()))
            .displayItems((parameters,output)->{
                output.accept(RAIL_ITEM.get());output.accept(LANDING_DOOR_ITEM.get());output.accept(CABIN_ITEM.get());
                output.accept(HIGH_SPEED_CABIN_ITEM.get());output.accept(OBSERVATION_CABIN_ITEM.get());output.accept(POWERFUL_CABIN_ITEM.get());
            }).build());

    /** 返回本模组的资源位置，不访问注册表。 */
    public static ResourceLocation id(String path){return new ResourceLocation(MOD_ID,path);}
    private static <T extends Entity> EntityType<T> cabinType(String name,EntityType.EntityFactory<T> factory){
        return EntityType.Builder.of(factory,MobCategory.MISC).sized(3,3).clientTrackingRange(10).updateInterval(1).build(id(name).toString());
    }
    /** 构造期仅把延迟注册器挂到 MOD 事件总线；网络在共同初始化队列中接线。 */
    public Easyelevator(){
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);SOUNDS.register(bus);TABS.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(ElevatorNetworking::register));
    }
}
