package com.brandon3055.brandonscore.client.render;

import com.brandon3055.brandonscore.BrandonsCore;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * PORT(1.19.2) WP-13: optional, reflection-only queries of Oculus (the Forge port of Iris shaders, mod id {@code oculus}).
 * There is no compile or runtime dependency: if Oculus is absent, or its public API ({@code net.irisshaders.iris.api.v0.IrisApi})
 * cannot be resolved, every query answers {@code false}.
 */
public final class ShaderModCompat {

    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";

    private static boolean resolved = false;
    private static MethodHandle shadowPass; // ()Z, bound to the IrisApi instance
    private static MethodHandle packInUse;  // ()Z, bound to the IrisApi instance

    private ShaderModCompat() {
    }

    public static boolean isShaderModLoaded() {
        ModList list = ModList.get();
        return list != null && (list.isLoaded("oculus") || list.isLoaded("iris"));
    }

    private static void resolve() {
        resolved = true;
        if (!isShaderModLoaded()) return;
        try {
            Class<?> api = Class.forName(IRIS_API);
            Object instance = api.getMethod("getInstance").invoke(null);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            MethodType bool = MethodType.methodType(boolean.class);
            shadowPass = lookup.findVirtual(api, "isRenderingShadowPass", bool).bindTo(instance);
            packInUse = lookup.findVirtual(api, "isShaderPackInUse", bool).bindTo(instance);
        } catch (Throwable e) {
            shadowPass = null;
            packInUse = null;
            BrandonsCore.LOGGER.warn("[1.19.2 port] Oculus is loaded but its API {} could not be resolved ({}); shadow-pass detection is off.", IRIS_API, e.toString());
        }
    }

    /**
     * @return true while Oculus renders its shadow pass (only possible with a shader pack in use).
     */
    public static boolean isRenderingShadowPass() {
        if (!resolved) resolve();
        if (shadowPass == null) return false;
        try {
            return (boolean) shadowPass.invokeExact();
        } catch (Throwable e) {
            shadowPass = null;
            return false;
        }
    }

    /**
     * @return true when Oculus is loaded and a shader pack is active.
     */
    public static boolean isShaderPackInUse() {
        if (!resolved) resolve();
        if (packInUse == null) return false;
        try {
            return (boolean) packInUse.invokeExact();
        } catch (Throwable e) {
            packInUse = null;
            return false;
        }
    }

    /**
     * R2 (spec phase5-ic2-compat §2.3 / §2.9): log-only hint, no behaviour change. While a shader pack is active,
     * Oculus 1.6.x masks colour and depth writes of every non-Iris core shader in the world pass, so BrandonsCore /
     * Draconic Evolution custom-shader geometry is not drawn. Oculus has no option to allow such shaders.
     */
    public static void logStartupHints() {
        if (!isShaderModLoaded()) return;
        BrandonsCore.LOGGER.info("[1.19.2 port] Oculus detected. While a shader pack is active, Oculus does not draw custom core shaders in the world: "
                + "Draconic Evolution reactor / energy core effects, crystals, chaos crystal, guardian shield, 3D modular tools and armor effects "
                + "and BrandonsCore contributor effects will be invisible (GUIs are not affected). Mitigation: disable the shader pack, or set "
                + "Client.fancyToolModels=false and Client.guardianShaders=false in config/brandon3055/DraconicEvolution.cfg.");
    }
}
