package com.cmdpro.databank.multiblock;

import com.cmdpro.databank.multiblock.predicates.AnyMultiblockPredicate;
import com.cmdpro.databank.multiblock.predicates.BlockstateMultiblockPredicate;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class Multiblock {
    public Map<BlockPos, BlockEntity> blockEntityCache = new Object2ObjectOpenHashMap<>();
    public String[][] multiblockLayers;
    public Map<Character, MultiblockPredicate> key;
    public BlockPos center;
    private List<List<List<PredicateAndPos>>> states;
    private List<PredicateAndPos> statesForMultiblockChecks;
    public Multiblock(String[][] multiblockLayers, Map<Character, MultiblockPredicate> key, BlockPos center) {
        this.multiblockLayers = multiblockLayers;
        key.put(' ', new BlockstateMultiblockPredicate(Blocks.AIR.defaultBlockState()));
        key.put('*', new AnyMultiblockPredicate());
        this.key = key;
        this.center = center;
    }
    public List<List<List<PredicateAndPos>>> getStates() {
        return getStates(false);
    }
    public List<List<List<PredicateAndPos>>> getStates(boolean forceCacheReset) {
        if (forceCacheReset || this.states == null) {
            int x = 0;
            int y = 0;
            int z = 0;
            List<List<List<PredicateAndPos>>> states = new ArrayList<>();
            for (String[] i : multiblockLayers) {
                z = 0;
                List<List<PredicateAndPos>> states2 = new ArrayList<>();
                for (String o : i) {
                    List<PredicateAndPos> layer = new ArrayList<>();
                    x = 0;
                    for (char p : o.toCharArray()) {
                        layer.add(new PredicateAndPos(key.get(p), new BlockPos(x, y, z).offset(center.getX(), center.getY(), center.getZ())));
                        x++;
                    }
                    states2.add(layer);
                    z++;
                }
                states.add(states2);
                y++;
            }
            this.states = states;
            return states;
        } else {
            return this.states;
        }
    }
    private List<PredicateAndPos> getStatesForMultiblockCheck() {
        return getStatesForMultiblockCheck(false);
    }
    private List<PredicateAndPos> getStatesForMultiblockCheck(boolean forceCacheReset) {
        if (forceCacheReset || this.statesForMultiblockChecks == null) {
            List<List<List<PredicateAndPos>>> states = getStates();
            List<PredicateAndPos> statesForMultiChecks = new ArrayList<>();
            for (List<List<PredicateAndPos>> i : states) {
                for (List<PredicateAndPos> j : i) {
                    for (PredicateAndPos k : j) {
                        if (!(k.predicate instanceof AnyMultiblockPredicate)) {
                            statesForMultiChecks.add(k);
                        }
                    }
                }
            }
            statesForMultiblockChecks = statesForMultiChecks;
            return statesForMultiChecks;
        } else {
            return this.statesForMultiblockChecks;
        }
    }
    public Rotation getMultiblockRotation(Level level, BlockPos pos) {
        if (checkMultiblock(level, pos, Rotation.NONE)) {
            return Rotation.NONE;
        }
        if (checkMultiblock(level, pos, Rotation.COUNTERCLOCKWISE_90)) {
            return Rotation.COUNTERCLOCKWISE_90;
        }
        if (checkMultiblock(level, pos, Rotation.CLOCKWISE_180)) {
            return Rotation.CLOCKWISE_180;
        }
        if (checkMultiblock(level, pos, Rotation.CLOCKWISE_90)) {
            return Rotation.CLOCKWISE_90;
        }
        return null;
    }
    public boolean checkMultiblock(Level level, BlockPos pos) {
        return checkMultiblock(level, pos, Rotation.NONE);
    }
    public boolean checkMultiblockAll(Level level, BlockPos pos) {
        return checkMultiblock(level, pos, Rotation.NONE) || checkMultiblock(level, pos, Rotation.CLOCKWISE_90) || checkMultiblock(level, pos, Rotation.CLOCKWISE_180) || checkMultiblock(level, pos, Rotation.COUNTERCLOCKWISE_90);
    }
    public boolean checkMultiblock(Level level, BlockPos pos, Rotation rotation) {
        for (PredicateAndPos i : getStatesForMultiblockCheck()) {
            if (i.predicate == null) {
                continue;
            }
            BlockPos blockPos = i.offset.rotate(rotation).offset(pos.getX(), pos.getY(), pos.getZ());
            BlockState state = level.getBlockState(blockPos);
            if (!i.predicate.isSame(state, rotation)) {
                return false;
            }
        }
        return true;
    }
    public List<List<String>> getMultiblockLayersList() {
        return Arrays.stream(multiblockLayers).map((a) -> Arrays.stream(a).toList()).toList();
    }
    public static class PredicateAndPos {
        public PredicateAndPos(MultiblockPredicate predicate, BlockPos offset) {
            this.predicate = predicate;
            this.offset = offset;
        }
        public MultiblockPredicate predicate;
        public BlockPos offset;
    }
}
