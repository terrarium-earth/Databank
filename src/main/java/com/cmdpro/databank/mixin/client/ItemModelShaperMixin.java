package com.cmdpro.databank.mixin.client;

import com.cmdpro.databank.hidden.types.ItemHiddenType;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelManager.class)
public class ItemModelShaperMixin {
    @Inject(method = "getItemModel", at = @At(value = "HEAD"), cancellable = true)
    private void getItemModel(Identifier identifier, CallbackInfoReturnable<ItemModel> cir) {
        var newModel = ItemHiddenType.hiddenItemModel(identifier);

        if (newModel != null) {
            var manager = (ModelManager)(Object)this;
            cir.setReturnValue(manager.getItemModel(newModel));
        }
    }

    @Inject(method = "getItemProperties", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void getItemProperties(Identifier identifier, CallbackInfoReturnable<ClientItem.Properties> cir) {
        var newModel = ItemHiddenType.hiddenItemModel(identifier);

        if (newModel != null) {
            var manager = (ModelManager)(Object)this;
            cir.setReturnValue(manager.getItemProperties(newModel));
        }
    }
}
