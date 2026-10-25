# PORT_NOTES — BrandonsCore 1.19.2 (unofficial port)

Unofficial derivative port of BrandonsCore 3.2.1 (1.20.1, `master` @ `5450c53`) to Minecraft 1.19.2 / Forge 43.x, with
CodeChickenLib's Modular GUI embedded under `src/mgui` (see `README.md`). Not supported by brandon3055. This file lists
the port-only additions and the intentional deviations from upstream 1.20.1 behaviour that are not plain API renames.
Plan, specs and per-work-package logs live in the port workspace (`PORTING_PLAN.md`, `specs/`, `logs/WP-*.md`).
Started by WP-13 (2026-10-24); the earlier Phase-1 port decisions (WP-10, WP-10b, WP-11a, WP-11b) are recorded in
those logs, in particular `logs/WP-10-api-changes.md` for the Modular GUI API differences.

## Transparent block-entity pass vs Rubidium (WP-13, spec `phase5-ic2-compat` F1/R1)

Problem: `BCClientEventHandler.renderLevelStage` (`AFTER_PARTICLES`) calls `BlockEntityRendererTransparent.renderTransparent`
for block entities it finds by walking vanilla `LevelRenderer.renderChunksInFrustum` and `globalBlockEntities`. Rubidium
(Sodium for Forge 1.19.2) replaces vanilla chunk setup/compilation and never fills either list, so the pass never runs.
Draconic Evolution's reactor core draws everything (core, shield, beams) in that pass, so it is invisible under Rubidium.
On 1.20.1, Embeddium patches BC for this (Embeddium commit `cffea33`); Rubidium 0.6.2c has no such patch.

Fix (port-only, 3 new classes + 1 mixin entry):
- `mixin/BlockEntityRenderDispatcherMixin` (client): observe-only `@Inject` at HEAD of
  `BlockEntityRenderDispatcher.setupAndRender` (SRG `m_112284_`). That private method is reached only after the
  dispatcher's own checks (renderer, level, valid state, `shouldRender`) and after HEAD cancels by other mods
  (EntityCulling). Vanilla, Rubidium and Oculus all draw block entities through this dispatcher.
- `client/render/TransparentPassTracker`: remembers (identity, render order) the client-level block entities whose
  renderer is a `BlockEntityRendererTransparent`; skipped while Oculus renders its shadow pass; reset at
  `RenderTickEvent` START and on client level unload, so nothing is held longer than one frame and captures from
  GUI/item rendering never reach the next frame.
- `BCClientEventHandler.renderLevelStage`: the upstream vanilla-list walk is unchanged; each block entity is drawn at
  most once per pass; afterwards the captured block entities the walk did not reach are drawn with the same frustum,
  translation, light and `shouldRender` handling.

Behaviour:
- Vanilla chunk rendering: identical to upstream (the captured set is a subset of what the walk already drew).
  Only difference: a block entity that would appear twice in the vanilla lists is drawn once.
- Rubidium: block entities that Rubidium rendered this frame get their transparent draw (the walk finds nothing).
- No reference to Rubidium/Oculus classes; Oculus is queried by reflection only (`client/render/ShaderModCompat`,
  `net.irisshaders.iris.api.v0.IrisApi`), every failure means "not present".
- Log line once per session: `[1.19.2 port] Transparent BER pass: BlockEntityRenderDispatcher hook active (WP-13 R1).`
  (proves at runtime that the mixin is applied).
- Known edge: under EntityCulling a culled block entity whose renderer is not `shouldRenderOffScreen` no longer gets a
  transparent draw under Rubidium (it gets none from the dispatcher either). DE's only transparent renderer (reactor core)
  is `shouldRenderOffScreen`, so it is never culled.

## Oculus shader packs hide custom core shaders (WP-13, spec `phase5-ic2-compat` F2/R2)

Oculus 1.6.x masks colour and depth writes for every non-Iris `ShaderInstance` applied during the world pass while a
shader pack is active, and has no option to allow them. BC/DE custom-shader geometry is then invisible in the world
(GUIs unaffected). The port does **not** ship a compat mixin for this (it would also need Iris render-target binding and
private immediate buffers for DE block-entity draws; not verifiable without a client). Shipped measure: a log-only hint at
client setup when Oculus is loaded (`ShaderModCompat.logStartupHints`, no behaviour change). Mitigations for players:
turn the shader pack off, or set `Client.fancyToolModels=false` and `Client.guardianShaders=false` in
`config/brandon3055/DraconicEvolution.cfg`.

## VBO render types: VAO fix and immediate draws (fix/vbo-bind, BC 3.2.1.9008)

**VAO corruption (CCL 4.3.2).** Stock CodeChickenLib 4.3.2 `codechicken.lib.render.buffer.VBORenderType` uploads and
draws its `VertexBuffer` without `bind()`. On 1.19.2 a `VertexBuffer` owns a VAO, so the upload/draw goes into whatever
VAO is bound at that moment (vanilla's immediate-mode buffers). Symptoms seen in game: invisible items in GUIs, black or
garbage geometry after a DE chestpiece / fancy tool / contributor wing model was drawn. Fix:
`com.brandon3055.brandonscore.mgui.render.BindingVBORenderType` (LGPL `src/mgui` tree, header names its origin) is a
copy of the CCL 4.3.2 class that binds the buffer around `upload` and `drawWithShader` and unbinds afterwards, like
CCL 1.20.1 does. BC `ContributorModel` and the DE VBO users (chestpiece, tool models) use it instead of the CCL class.
CCL itself is unchanged (stock 4.3.2.490 jar).

**Immediate draws (`WithCallbacks.drawImmediate()`).** A VBO render type only draws inside `RenderType.end(...)`, which
vanilla's `MultiBufferSource.BufferSource` calls on `endBatch`. Oculus (even with no shader pack active) and
ImmediatelyFast replace the buffer sources with batching sources that draw the collected geometry themselves and never
call `end` on the type, so a VBO type handed to `buffers.getBuffer(...)` was never drawn (chestpiece missing on the
body, fancy 3D tools missing). All VBO submits now call `drawImmediate()` (setup state, draw, clear state) at the point
where the type used to be requested; the current `RenderSystem` matrices are the ones the batched draw would have used,
so the result is identical under vanilla. BC site: `ContributorModel.WingBoneRenderer`; DE sites: `ToolRenderBase`
(4) and `ModularChestpieceModel` (3). Verified in a dev client and in the user's pack (shaders off). With an Oculus
shader pack active the custom-shader models stay invisible (R2 above, not changed by this fix).

**Cursor reload.** mgui `CursorHelper.onResourceReload` destroyed all cached GLFW cursors, including the one GLFW was
using, which left the window without a cursor after a resource reload while a custom cursor was active. It now resets
the active cursor (`glfwSetCursor(window, NULL)`) before destroying the cached ones.
