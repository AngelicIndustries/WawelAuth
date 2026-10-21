package industries.angelic.wawelauth.compat.angelica;

import net.coderbot.iris.layer.GbufferPrograms;
import net.coderbot.iris.uniforms.CapturedRenderingState;
import net.coderbot.iris.uniforms.EntityIdHelper;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.entity.Entity;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.wawelauth.WawelAuth;

/**
 * Angelica's shader pipeline, for see-through skins. Held see-through texels are drawn after the translucent terrain,
 * outside Angelica's entity loop: they get its entities phase back, declared translucent so a shader pack draws them
 * with its translucent entity program, each tagged with its player as Angelica's own deferred entity overlays are.
 * Nothing is split during the shadow pass, whose transforms are the sun's.
 * <p>
 * Without Angelica every check is false. An Angelica without these classes turns them off once, with a warning.
 */
@SideOnly(Side.CLIENT)
public final class AngelicaShaders {

    private static final boolean LOADED = Loader.isModLoaded("angelica");
    private static boolean broken;

    private AngelicaShaders() {}

    /** Whether a shader pack is drawing the world. */
    public static boolean inUse() {
        if (!LOADED || broken) return false;
        try {
            return Pipeline.inUse();
        } catch (LinkageError error) {
            broke(error);
            return false;
        }
    }

    /** Whether the shader pack's shadow map is being drawn. */
    public static boolean shadowPass() {
        if (!LOADED || broken) return false;
        try {
            return Pipeline.shadowPass();
        } catch (LinkageError error) {
            broke(error);
            return false;
        }
    }

    /**
     * Opens the entities phase for draws made outside Angelica's entity loop, declared translucent. Null when no shader
     * pack is in use; pass the result to {@link #endEntities}.
     */
    public static Scope beginEntities() {
        if (!inUse()) return null;
        try {
            return Pipeline.beginEntities();
        } catch (IllegalStateException error) {
            // Angelica is in another phase: the draws take whichever program it has.
            return null;
        } catch (LinkageError error) {
            broke(error);
            return null;
        }
    }

    /** The entity the following draws belong to, for shader packs that tell players apart. */
    public static void entity(Scope scope, Entity entity) {
        if (scope == null) return;
        try {
            Pipeline.entity(entity);
        } catch (LinkageError error) {
            broke(error);
        }
    }

    public static void endEntities(Scope scope) {
        if (scope == null) return;
        try {
            Pipeline.endEntities(scope);
        } catch (LinkageError error) {
            broke(error);
        }
    }

    private static void broke(Throwable error) {
        if (broken) return;
        broken = true;
        WawelAuth.LOG
            .warn("Angelica's shader pipeline could not be reached; see-through skins ignore shader packs", error);
    }

    /** An open entities phase and the translucency declaration it replaced. */
    public static final class Scope {

        final Boolean declared;

        Scope(Boolean declared) {
            this.declared = declared;
        }
    }

    /** Angelica's classes, only loaded once Angelica is known to be there. */
    private static final class Pipeline {

        static boolean inUse() {
            return IrisApi.getInstance()
                .isShaderPackInUse();
        }

        static boolean shadowPass() {
            return IrisApi.getInstance()
                .isRenderingShadowPass();
        }

        static Scope beginEntities() {
            GbufferPrograms.beginEntities();
            return new Scope(GbufferPrograms.beginTranslucencyDeclaration(Boolean.TRUE));
        }

        static void entity(Entity entity) {
            CapturedRenderingState.INSTANCE.setCurrentEntityAndItem(EntityIdHelper.getEntityId(entity), 0);
        }

        static void endEntities(Scope scope) {
            CapturedRenderingState.INSTANCE.setCurrentEntityAndItem(-1, 0);
            GbufferPrograms.endTranslucencyDeclaration(scope.declared);
            GbufferPrograms.endEntities();
        }
    }
}
