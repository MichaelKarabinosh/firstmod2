package net.dannykandmichaelk.firstmod.entity.custom;

import net.dannykandmichaelk.firstmod.entity.ModEntities;
import net.dannykandmichaelk.firstmod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MrDasEntity extends Evoker implements  NeutralMob {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;
    public final AnimationState attackAnimationState = new AnimationState();
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private int ticksUntilNextAlert;
    private static final UniformInt ALERT_INTERVAL = TimeUtil.rangeOfSeconds(1, 2);
    private static final ResourceLocation SPEED_MODIFIER_ATTACKING_ID = ResourceLocation.withDefaultNamespace("attacking");
    private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(SPEED_MODIFIER_ATTACKING_ID, 0.05, AttributeModifier.Operation.ADD_VALUE);
    private int playFirstAngerSoundIn;
    private static final UniformInt FIRST_ANGER_SOUND_DELAY = TimeUtil.rangeOfSeconds(0, 1);



    public MrDasEntity(EntityType<? extends Evoker> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    protected void registerGoals() {
//        this.goalSelector.addGoal(0,new FloatGoal(this));


        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 3, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 1, false, true, this::isAngryAt));
//        this.targetSelector.addGoal(1, new SpellcasterCastingSpellGoal());
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpellcasterUseSpellGoal() {
            @Override
            protected void performSpellCasting() {
                LivingEntity livingentity = MrDasEntity.this.getTarget();
                double d0 = Math.min(livingentity.getY(), MrDasEntity.this.getY());
                double d1 = Math.max(livingentity.getY(), MrDasEntity.this.getY()) + 1.0;
                float f = (float) Mth.atan2(livingentity.getZ() - MrDasEntity.this.getZ(), livingentity.getX() - MrDasEntity.this.getX());
                if (MrDasEntity.this.distanceToSqr(livingentity) < 9.0) {
                    for (int i = 0; i < 5; i++) {
                        float f1 = f + (float) i * (float) Math.PI * 0.4F;
                        this.createSpellEntity(
                                MrDasEntity.this.getX() + (double) Mth.cos(f1) * 1.5, MrDasEntity.this.getZ() + (double) Mth.sin(f1) * 1.5, d0, d1, f1, 0
                        );
                    }

                    for (int k = 0; k < 8; k++) {
                        float f2 = f + (float) k * (float) Math.PI * 2.0F / 8.0F + (float) (Math.PI * 2.0 / 5.0);
                        this.createSpellEntity(
                                MrDasEntity.this.getX() + (double) Mth.cos(f2) * 2.5, MrDasEntity.this.getZ() + (double) Mth.sin(f2) * 2.5, d0, d1, f2, 3
                        );
                    }
                } else {
                    for (int l = 0; l < 16; l++) {
                        double d2 = 1.25 * (double) (l + 1);
                        int j = 1 * l;
                        this.createSpellEntity(MrDasEntity.this.getX() + (double) Mth.cos(f) * d2, MrDasEntity.this.getZ() + (double) Mth.sin(f) * d2, d0, d1, f, j);
                    }
                }
            }

            private void createSpellEntity(double pX, double pZ, double pMinY, double pMaxY, float pYRot, int pWarmupDelay) {
                BlockPos blockpos = BlockPos.containing(pX, pMaxY, pZ);
                boolean flag = false;
                double d0 = 0.0;

                do {
                    BlockPos blockpos1 = blockpos.below();
                    BlockState blockstate = MrDasEntity.this.level().getBlockState(blockpos1);
                    if (blockstate.isFaceSturdy(MrDasEntity.this.level(), blockpos1, Direction.UP)) {
                        if (!MrDasEntity.this.level().isEmptyBlock(blockpos)) {
                            BlockState blockstate1 = MrDasEntity.this.level().getBlockState(blockpos);
                            VoxelShape voxelshape = blockstate1.getCollisionShape(MrDasEntity.this.level(), blockpos);
                            if (!voxelshape.isEmpty()) {
                                d0 = voxelshape.max(Direction.Axis.Y);
                            }
                        }

                        flag = true;
                        break;
                    }

                    blockpos = blockpos.below();
                } while (blockpos.getY() >= Mth.floor(pMinY) - 1);

                if (flag) {
                    MrDasEntity.this.level()
                            .addFreshEntity(new WerewolfEntity(EntityType.BEE, MrDasEntity.this.level()));
                    MrDasEntity.this.level()
                            .gameEvent(GameEvent.ENTITY_PLACE, new Vec3(pX, (double) blockpos.getY() + d0, pZ), GameEvent.Context.of(MrDasEntity.this));
                }
            }

            @Override
            protected int getCastingTime() {
                return 0;
            }

            @Override
            protected int getCastingInterval() {
                return 0;
            }

            @Nullable
            @Override
            protected SoundEvent getSpellPrepareSound() {
                return null;
            }

            @Override
            protected IllagerSpell getSpell() {
                return null;
            }

        });

        this.goalSelector.addGoal(3, new TemptGoal(this, 1.125, stack -> stack.is(ModItems.TRUMPIUM.get()), false));



        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

    }



    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25F)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 150.0F)
                .add(Attributes.ATTACK_DAMAGE,3)
                .add(Attributes.ATTACK_SPEED,10F)
                .add(Attributes.ATTACK_KNOCKBACK,10)
                .add(Attributes.FALL_DAMAGE_MULTIPLIER,0)
                .add(Attributes.STEP_HEIGHT, 4.0)
                .add(Attributes.FOLLOW_RANGE,1000)
                .add(Attributes.SCALE, 1.5F);
    }

    @Override
    public SoundEvent getEatingSound(ItemStack pStack) {
        return SoundEvents.WITHER_HURT;
    }

    private void playAngerSound() {
        this.playSound(SoundEvents.RAVAGER_ROAR, this.getSoundVolume() * 50.0F, this.getVoicePitch() * 0.01F);
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pState) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.15F, 0.05F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.BONE_BLOCK_BREAK;
    }



    private void setupAnimationStates() {
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 40;
            this.idleAnimationState.start(this.tickCount);
        }
        else {
            --this.idleAnimationTimeout;
        }
    }




    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setRemainingPersistentAngerTime(int pTime) {
        this.remainingPersistentAngerTime = pTime;
    }

    @Override
    public @Nullable UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID pPersistentAngerTarget) {
        this.persistentAngerTarget = pPersistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    private void maybeAlertOthers() {
        if (this.ticksUntilNextAlert > 0) {
            this.ticksUntilNextAlert--;
        } else {
            if (this.getSensing().hasLineOfSight(this.getTarget())) {
                this.alertOthers();
            }

            this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
        }
    }

    private void maybePlayFirstAngerSound() {
        if (this.playFirstAngerSoundIn > 0) {
            this.playFirstAngerSoundIn--;
            if (this.playFirstAngerSoundIn == 0) {
                this.playAngerSound();
            }
        }
    }

    @Override
    public void setTarget(@javax.annotation.Nullable LivingEntity pLivingEntity) {
        if (this.getTarget() == null && pLivingEntity != null) {
            this.playFirstAngerSoundIn = FIRST_ANGER_SOUND_DELAY.sample(this.random);
            this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
        }

        if (pLivingEntity instanceof Player) {
            this.setLastHurtByPlayer((Player)pLivingEntity);
        }

        super.setTarget(pLivingEntity);
    }
    public void tick() {
        super.tick();

        if(this.level().isClientSide()) {
            this.setupAnimationStates();
        }
    }

    protected void customServerAiStep() {

        this.maybePlayFirstAngerSound();


        this.updatePersistentAnger((ServerLevel)this.level(), true);
        if (this.getTarget() != null) {
            this.maybeAlertOthers();
        }

        if (this.isAngry()) {
            this.lastHurtByPlayerTime = this.tickCount;
            this.attackAnimationState.startIfStopped(this.tickCount);
        }

        super.customServerAiStep();
    }

    private void alertOthers() {
        double d0 = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB aabb = AABB.unitCubeFromLowerCorner(this.position()).inflate(d0, 10.0, d0);
        this.level()
                .getEntitiesOfClass(MrDasEntity.class, aabb, EntitySelector.NO_SPECTATORS)
                .stream()
                .filter(p_327015_ -> p_327015_.getTarget() == null)
                .filter(p_327016_ -> !p_327016_.isAlliedTo(this.getTarget()))
                .forEach(p_327014_ -> p_327014_.setTarget(this.getTarget()));
    }










    private void createSpellEntity(double pX, double pZ, double pMinY, double pMaxY, float pYRot, int pWarmupDelay) {
        BlockPos blockpos = BlockPos.containing(pX, pMaxY, pZ);
        boolean flag = false;
        double d0 = 0.0;

        do {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = MrDasEntity.this.level().getBlockState(blockpos1);
            if (blockstate.isFaceSturdy(MrDasEntity.this.level(), blockpos1, Direction.UP)) {
                if (!MrDasEntity.this.level().isEmptyBlock(blockpos)) {
                    BlockState blockstate1 =MrDasEntity.this.level().getBlockState(blockpos);
                    VoxelShape voxelshape = blockstate1.getCollisionShape(MrDasEntity.this.level(), blockpos);
                    if (!voxelshape.isEmpty()) {
                        d0 = voxelshape.max(Direction.Axis.Y);
                    }
                }

                flag = true;
                break;
            }

            blockpos = blockpos.below();
        } while (blockpos.getY() >= Mth.floor(pMinY) - 1);

        if (flag) {
            MrDasEntity.this.level()
                    .addFreshEntity(new EvokerFangs(MrDasEntity.this.level(), pX, (double)blockpos.getY() + d0, pZ, pYRot, pWarmupDelay, MrDasEntity.this));
            MrDasEntity.this.level()
                    .gameEvent(GameEvent.ENTITY_PLACE, new Vec3(pX, (double)blockpos.getY() + d0, pZ), GameEvent.Context.of(MrDasEntity.this));
        }
    }


}
