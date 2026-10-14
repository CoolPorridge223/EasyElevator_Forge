package org.DJB.easyelevator.logic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;
import java.util.function.Predicate;

/**
 * 原版实体碰撞查询的委托视图，只转发 EntityGetter 的三个抽象查询接口。
 * 此类没有覆写 getEntityCollisions，调用时由 JVM 正常解析原版接口默认实现。
 * 不在 Mixin 内执行 EntityGetter.super：Mixin 0.8.5 会把该 invokespecial 的 owner
 * 改写成 Level，却保留接口常量池类型，导致玩家出生时抛出 IncompatibleClassChangeError。
 */
public final class VanillaEntityCollisions implements EntityGetter {
    private final EntityGetter delegate;
    private VanillaEntityCollisions(EntityGetter delegate){this.delegate=delegate;}

    /** 复用原版过滤及形状生成逻辑，避免复制原版算法，也不回调 Level 的碰撞入口。 */
    public static List<VoxelShape> query(EntityGetter world,Entity entity,AABB box){
        return new VanillaEntityCollisions(world).getEntityCollisions(entity,box);
    }

    /** 转发原版按实体及包围盒查询；保持谓词和排除实体不变。 */
    @Override public List<Entity> getEntities(Entity except,AABB box,Predicate<? super Entity> predicate){
        return delegate.getEntities(except,box,predicate);
    }

    /** 转发按实体类型的查询，供接口默认方法使用。 */
    @Override public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity,T> type,AABB box,Predicate<? super T> predicate){
        return delegate.getEntities(type,box,predicate);
    }

    /** 返回所属世界的玩家列表，不创建或缓存额外玩家状态。 */
    @Override public List<? extends Player> players(){return delegate.players();}
}
