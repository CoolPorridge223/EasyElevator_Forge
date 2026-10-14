package org.DJB.easyelevator.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.DJB.easyelevator.entity.AbstractCabinEntity;
import org.DJB.easyelevator.logic.VanillaEntityCollisions;
import org.spongepowered.asm.mixin.Mixin;
import java.util.ArrayList;
import java.util.List;

/**
 * 为 Level 提供实体碰撞查询：保留原版结果，再追加电梯空心外壳。
 * 1.20.1 的 Level 原本继承 EntityGetter 默认实现，因此这里增加的是具体实现方法。
 * 原版查询放在非 Mixin 辅助类，禁止在此使用接口 super 调用，避免运行期方法引用类型错误。
 */
@Mixin(Level.class)
public abstract class EntityViewMixin implements EntityGetter {
    /**
     * 不移动实体、不修改世界。轿厢自身跳过外壳追加，避免被自己的地板和侧壁卡住。
     * 没有附近轿厢时直接返回原版列表；有轿厢时复制列表，避免修改共享结果。
     */
    @Override public List<VoxelShape> getEntityCollisions(Entity entity,AABB box){
        EntityGetter world=(EntityGetter)this;
        List<VoxelShape> original=VanillaEntityCollisions.query(world,entity,box);
        if(entity instanceof AbstractCabinEntity)return original;
        var cabins=world.getEntitiesOfClass(AbstractCabinEntity.class,box.inflate(.001),c->!c.isRemoved());
        if(cabins.isEmpty())return original;
        List<VoxelShape> result=new ArrayList<>(original);
        for(var cabin:cabins)for(AABB part:cabin.collisionBoxes())
            if(part.intersects(box))result.add(Shapes.create(part));
        return result;
    }
}
