package com.cmdpro.databank.megastructures.block;

import com.cmdpro.databank.Databank;
import com.cmdpro.databank.megastructures.Megastructure;
import com.cmdpro.databank.registry.AttachmentTypeRegistry;
import com.cmdpro.databank.registry.BlockEntityRegistry;
import com.cmdpro.databank.registry.BlockRegistry;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FileUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplatePathFactory;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Databank.MOD_ID)
public class MegastructureSaveBlockEntity extends BlockEntity implements BoundingBoxRenderable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter LISTER = new FileToIdConverter("megastructures", "json");

    int changeProgressTo;
    private int bindProcess;
    public BlockPos corner1;
    public BlockPos corner2;
    public BlockPos center;
    private UUID uuid;

    public MegastructureSaveBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.MEGASTRUCTURE_SAVE.get(), pos, blockState);
    }

    public void setBindProcess(int process) {
        changeProgressTo = process;
    }
    public boolean isChangingBind() {
        return bindProcess != changeProgressTo;
    }
    public int getBindProcess() {
        return bindProcess;
    }

    public UUID getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }
    public void tick(Level level, BlockPos pos, BlockState state) {
        if (changeProgressTo != bindProcess) {
            bindProcess = changeProgressTo;
        }
    }
    protected void updateBlock() {
        BlockState blockState = level.getBlockState(this.getBlockPos());
        this.level.sendBlockUpdated(this.getBlockPos(), blockState, blockState, 3);
        this.setChanged();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        Optional<BlockPos> corner1 = valueInput.read("corner1", BlockPos.CODEC);
        Optional<BlockPos> corner2 = valueInput.read("corner2", BlockPos.CODEC);
        Optional<BlockPos> center = valueInput.read("center", BlockPos.CODEC);

        corner1.ifPresent(blockPos -> this.corner1 = blockPos);
        corner2.ifPresent(blockPos -> this.corner2 = blockPos);
        center.ifPresent(blockPos -> this.center = blockPos);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problems, pRegistries);

        output.storeNullable("corner1", BlockPos.CODEC, corner1);
        output.storeNullable("corner2", BlockPos.CODEC, corner2);
        output.storeNullable("center", BlockPos.CODEC, center);

        problems.forEach((path, problem) -> LOGGER.warn("Problem serializing megastructure save block entity {} to network in {}, {}", getBlockPos(), path, problem));

        return output.buildResult();
    }
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                if (getBindProcess() == 0) {
                    player.sendSystemMessage(Component.translatable("block.databank.megastructure_save.corner1"));
                    player.setData(AttachmentTypeRegistry.BINDING_BLOCK, Optional.of(this));
                    setBindProcess(1);
                } else {
                    corner1 = null;
                    corner2 = null;
                    center = null;
                    player.setData(AttachmentTypeRegistry.BINDING_BLOCK, Optional.empty());
                    player.sendSystemMessage(Component.translatable("block.databank.megastructure_save.reset"));
                    updateBlock();
                    setBindProcess(0);
                }
            } else {
                if (corner1 != null && corner2 != null && center != null) {
                    save();
                    player.sendSystemMessage(Component.translatable("block.databank.megastructure_save.saved", uuid.toString()));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
    public void save() {
        Megastructure megastructure = Megastructure.createFromWorld(level, corner1, corner2, center);
        try {
            Path root = ((ServerLevel) level).getServer().getWorldPath(LevelResource.GENERATED_DIR).normalize();
            var pathFactory = new TemplatePathFactory(root);
            Path path = pathFactory.createAndValidatePathToStructure(Databank.locate(getUuid().toString()), LISTER);
            JsonElement json = Megastructure.CODEC.encode(megastructure, JsonOps.INSTANCE, JsonOps.INSTANCE.mapBuilder()).build(JsonOps.INSTANCE.empty()).result().orElse(null);

            if (json != null) {
                Path parentPath = path.getParent();
                Files.createDirectories(Files.exists(parentPath) ? parentPath.toRealPath() : parentPath);
                Files.deleteIfExists(path);
                Files.writeString(path, json.toString());
            }
        } catch (Exception e) {
            Databank.LOGGER.trace(e.getMessage(), e.fillInStackTrace());
        }
    }
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            if (!event.getLevel().getBlockState(event.getPos()).is(BlockRegistry.MEGASTRUCTURE_SAVE.get())) {
                event.getEntity().getData(AttachmentTypeRegistry.BINDING_BLOCK).ifPresent((binding) -> {
                    if (binding instanceof MegastructureSaveBlockEntity ent) {
                        if (!ent.isChangingBind()) {
                            if (ent.getBindProcess() == 1) {
                                ent.corner1 = event.getPos();
                                event.getEntity().sendSystemMessage(Component.translatable("block.databank.megastructure_save.corner2"));
                                ent.updateBlock();
                                ent.setBindProcess(2);
                            } else if (ent.getBindProcess() == 2) {
                                ent.corner2 = event.getPos();
                                event.getEntity().sendSystemMessage(Component.translatable("block.databank.megastructure_save.center", ent.corner1.toShortString(), ent.corner2.toShortString()));
                                ent.updateBlock();
                                ent.setBindProcess(3);
                            } else if (ent.getBindProcess() == 3) {
                                ent.center = event.getPos();
                                event.getEntity().setData(AttachmentTypeRegistry.BINDING_BLOCK, Optional.empty());
                                event.getEntity().sendSystemMessage(Component.translatable("block.databank.megastructure_save.finished", ent.corner1.toShortString(), ent.corner2.toShortString()));
                                ent.updateBlock();
                                ent.setBindProcess(0);
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("uuid", UUIDUtil.CODEC, getUuid());
        output.storeNullable("corner1", BlockPos.CODEC, corner1);
        output.storeNullable("corner2", BlockPos.CODEC, corner2);
        output.storeNullable("center", BlockPos.CODEC, center);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        Optional<UUID> uuid = input.read("uuid", UUIDUtil.CODEC);
        Optional<BlockPos> corner1 = input.read("corner1", BlockPos.CODEC);
        Optional<BlockPos> corner2 = input.read("corner2", BlockPos.CODEC);
        Optional<BlockPos> center = input.read("center", BlockPos.CODEC);

        uuid.ifPresent((id) -> this.uuid = id);
        corner1.ifPresent(blockPos -> this.corner1 = blockPos);
        corner2.ifPresent(blockPos -> this.corner2 = blockPos);
        center.ifPresent(blockPos -> this.center = blockPos);
    }

    @Override
    public Mode renderMode() {
        return corner1 != null && corner2 != null ? Mode.BOX_AND_INVISIBLE_BLOCKS : Mode.BOX;
    }

    @Override
    public RenderableBox getRenderableBox() {
        return new RenderableBox(corner1, corner2.subtract(corner1));
    }
}
