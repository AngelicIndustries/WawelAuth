package industries.angelic.wawelauth.client.skin.voxel;

import java.util.List;

import industries.angelic.wawelauth.api.skin.LayerSegment;

/** Builds the voxel layer of a {@link LayerSegment}, in the segment's part space. */
public final class VoxelMeshes {

    private VoxelMeshes() {}

    /** The cubes of a segment; empty when none of its texels are visible. */
    public static List<VoxelCube> cubes(VoxelTexture texture, LayerSegment segment) {
        VoxelCubeCollector collector = new VoxelCubeCollector();
        wrap(collector, texture, segment);
        return collector.getCubes();
    }

    /** Builds and compiles a segment's mesh. Render thread only; null when none of its texels are visible. */
    public static VoxelMesh compile(VoxelTexture texture, LayerSegment segment) {
        List<VoxelCube> cubes = cubes(texture, segment);
        if (cubes.isEmpty()) return null;
        VoxelMesh mesh = new VoxelMesh(cubes);
        mesh.compileDisplayList();
        return mesh;
    }

    static void wrap(VoxelBuilder builder, VoxelTexture texture, LayerSegment segment) {
        VoxelSurfaceBuilder.wrapSlice(
            builder,
            texture,
            segment.width(),
            segment.height(),
            segment.depth(),
            segment.u(),
            segment.v(),
            segment.rowStart(),
            segment.rowEnd(),
            segment.originX(),
            segment.originY() + segment.rowStart(),
            segment.originZ());
    }
}
