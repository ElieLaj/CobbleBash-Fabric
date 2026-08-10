package com.nore.cobblebash.beacon;

import com.nore.cobblebash.CobbleBash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ChampionBeaconBlockEntity extends BlockEntity implements MenuProvider {
   public static final int MAX_LEVELS = 5;
   public static final int RADIUS_PER_LEVEL = 10;
   public static final int BASE_RADIUS = 10;
   public static final TagKey<Block> BASE_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("cobblemon", "evolution_stone_blocks"));
   private static final Component DEFAULT_NAME = Component.translatable("container.cobblebash.champion_beacon");
   private static final String TAG_LEVELS = "Levels";
   private static final String TAG_BEAM_CLEAR = "BeamClear";
   private static final String TAG_PRIMARY = "PrimaryPower";
   private static final String TAG_SECONDARY = "SecondaryPower";
   private static final String TAG_UPGRADED = "Upgraded";
   private static final String TAG_PAYMENT_ITEM = "PaymentItem";
   private int levels;
   private boolean beamClear;
   private ChampionBeaconPower primaryPower = ChampionBeaconPower.NONE;
   private ChampionBeaconPower secondaryPower = ChampionBeaconPower.NONE;
   private boolean upgraded;
   private ResourceLocation paymentItem = ResourceLocation.fromNamespaceAndPath("cobblemon", "water_stone");

   public ChampionBeaconBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)CobbleBash.CHAMPION_BEACON_BLOCK_ENTITY, pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ChampionBeaconBlockEntity blockEntity) {
      if (!level.isClientSide) {
         if (level.getGameTime() % 80L == 0L) {
            blockEntity.refreshLevels();
            ChampionBeaconAuras.track(blockEntity);
            if (blockEntity.hasBeam()) {
               blockEntity.playSound(SoundEvents.BEACON_AMBIENT);
            }
         }

         ChampionBeaconAuras.tickBeacon(blockEntity);
      }
   }

   public void refreshLevels() {
      if (this.level != null) {
         int i = this.levels;
         boolean flag = this.beamClear;
         this.levels = calculateBaseLevels(this.level, this.worldPosition);
         this.beamClear = this.levels > 0 && hasClearBeam(this.level, this.worldPosition);
         if (i != this.levels || flag != this.beamClear) {
            if (i <= 0 && this.levels > 0 && this.beamClear) {
               this.playSound(SoundEvents.BEACON_ACTIVATE);
            } else if (i > 0 && (this.levels <= 0 || !this.beamClear)) {
               this.playSound(SoundEvents.BEACON_DEACTIVATE);
            }

            this.setChangedAndSync();
         }
      }
   }

   private static int calculateBaseLevels(Level level, BlockPos pos) {
      int i = 0;

      for (int j = 1; j <= 5; i = j++) {
         int k = pos.getY() - j;
         if (k < level.getMinBuildHeight()) {
            break;
         }

         boolean flag = true;

         for (int l = pos.getX() - j; l <= pos.getX() + j && flag; l++) {
            for (int i1 = pos.getZ() - j; i1 <= pos.getZ() + j; i1++) {
               if (!level.getBlockState(new BlockPos(l, k, i1)).is(BASE_BLOCKS)) {
                  flag = false;
                  break;
               }
            }
         }

         if (!flag) {
            break;
         }
      }

      return i;
   }

   private static boolean hasClearBeam(Level level, BlockPos pos) {
      int i = level.getHeight();

      for (BlockPos blockpos = pos.above(); blockpos.getY() < i; blockpos = blockpos.above()) {
         BlockState blockstate = level.getBlockState(blockpos);
         if (beaconColor(blockstate) == null
            && blockstate.getLightBlock(level, blockpos) >= 15
            && !blockstate.is(Blocks.BEDROCK)) {
            return false;
         }
      }

      return true;
   }

   public int getLevels() {
      return this.levels;
   }

   public int getRadius() {
      return this.levels <= 0 ? 0 : 10 + this.levels * 10;
   }

   public boolean hasBeam() {
      return this.levels > 0 && this.beamClear;
   }

   public ChampionBeaconPower getPrimaryPower() {
      return this.primaryPower;
   }

   public ChampionBeaconPower getSecondaryPower() {
      return this.secondaryPower;
   }

   public boolean isUpgraded() {
      return this.upgraded;
   }

   public void applyPowers(ChampionBeaconPower primaryPower, ChampionBeaconPower secondaryPower, boolean upgraded) {
      this.applyPowers(primaryPower, secondaryPower, upgraded, this.paymentItem);
   }

   public void applyPowers(ChampionBeaconPower primaryPower, ChampionBeaconPower secondaryPower, boolean upgraded, ResourceLocation paymentItem) {
      this.primaryPower = primaryPower.isPrimary() ? primaryPower : ChampionBeaconPower.NONE;
      this.secondaryPower = secondaryPower.isSecondary() && !conflicts(this.primaryPower, secondaryPower) ? secondaryPower : ChampionBeaconPower.NONE;
      this.upgraded = upgraded && this.primaryPower.isUpgradeable();
      this.paymentItem = paymentItem == null ? ResourceLocation.fromNamespaceAndPath("cobblemon", "water_stone") : paymentItem;
      this.playSound(SoundEvents.BEACON_POWER_SELECT);
      this.setChangedAndSync();
      ChampionBeaconAuras.track(this);
   }

   public ResourceLocation getPaymentItem() {
      return this.paymentItem;
   }

   private void playSound(SoundEvent sound) {
      if (this.level != null) {
         this.level.playSound(null, this.worldPosition, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   private void setChangedAndSync() {
      this.setChanged();
      if (this.level != null) {
         BlockState blockstate = this.getBlockState();
         this.level.sendBlockUpdated(this.worldPosition, blockstate, blockstate, 3);
      }
   }

   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      this.levels = tag.getInt("Levels");
      this.beamClear = tag.getBoolean("BeamClear");
      this.primaryPower = ChampionBeaconPower.byId(tag.getInt("PrimaryPower"));
      this.secondaryPower = ChampionBeaconPower.byId(tag.getInt("SecondaryPower"));
      if (conflicts(this.primaryPower, this.secondaryPower)) {
         this.secondaryPower = ChampionBeaconPower.NONE;
      }

      this.upgraded = tag.getBoolean("Upgraded");
      if (tag.contains("PaymentItem")) {
         this.paymentItem = ResourceLocation.tryParse(tag.getString("PaymentItem"));
         if (this.paymentItem == null) {
            this.paymentItem = ResourceLocation.fromNamespaceAndPath("cobblemon", "water_stone");
         }
      }
   }

   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("Levels", this.levels);
      tag.putBoolean("BeamClear", this.beamClear);
      tag.putInt("PrimaryPower", this.primaryPower.id());
      tag.putInt("SecondaryPower", this.secondaryPower.id());
      tag.putBoolean("Upgraded", this.upgraded);
      tag.putString("PaymentItem", this.paymentItem.toString());
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag(Provider registries) {
      return this.saveCustomOnly(registries);
   }

   public Component getDisplayName() {
      return DEFAULT_NAME;
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      this.refreshLevels();
      return new ChampionBeaconMenu(containerId, playerInventory, ContainerLevelAccess.create(this.level, this.worldPosition), this);
   }

   public void setRemoved() {
      ChampionBeaconAuras.untrack(this.level, this.worldPosition);
      super.setRemoved();
   }

   private static boolean conflicts(ChampionBeaconPower primary, ChampionBeaconPower secondary) {
      return primary == ChampionBeaconPower.REPEL && secondary == ChampionBeaconPower.LURE
         || primary == ChampionBeaconPower.LURE && secondary == ChampionBeaconPower.REPEL;
   }

   /**
    * Couleur que ce bloc donne au faisceau, ou null s'il l'arrete.
    *
    * <p>Remplace {@code BlockState.getBeaconColorMultiplier}, ajoute a vanilla
    * par NeoForge. La balise vanilla fait le meme test : seul un
    * {@link BeaconBeamBlock} teinte le faisceau.
    */
   private static Integer beaconColor(BlockState state) {
      return state.getBlock() instanceof BeaconBeamBlock beam
         ? beam.getColor().getTextureDiffuseColor()
         : null;
   }

}
