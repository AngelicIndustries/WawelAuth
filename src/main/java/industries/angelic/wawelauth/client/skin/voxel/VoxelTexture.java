package industries.angelic.wawelauth.client.skin.voxel;

public interface VoxelTexture {

    boolean isPresent(VoxelSurfaceBuilder.UV onTextureUV);

    boolean isSolid(VoxelSurfaceBuilder.UV onTextureUV);

    int getWidth();

    int getHeight();

}
