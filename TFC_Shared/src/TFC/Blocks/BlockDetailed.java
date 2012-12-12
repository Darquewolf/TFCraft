package TFC.Blocks;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.asm.SideOnly;

import TFC.*;
import TFC.Core.*;
import TFC.Core.Player.PlayerInfo;
import TFC.Core.Player.PlayerManagerTFC;
import TFC.Items.ItemChisel;
import TFC.Items.ItemHammer;
import TFC.TileEntities.TileEntityDetailed;
import TFC.TileEntities.TileEntityPartial;
import TFC.TileEntities.TileEntityWoodConstruct;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Material;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;

public class BlockDetailed extends BlockPartial
{
	public BlockDetailed(int par1)
	{
		super(par1, Material.rock);
	}

	@Override
	public int getRenderType()
	{
		return TFCBlocks.detailedRenderId;
	}

	@Override
	public boolean getBlocksMovement(IBlockAccess par1IBlockAccess, int i, int j, int k)
	{
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World var1) {
		// TODO Auto-generated method stub
		return new TileEntityDetailed();
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess par1IBlockAccess, int par2, int par3, int par4, int par5)
    {
        return true;
    }

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityplayer, int side, float hitX, float hitY, float hitZ) 
	{
		boolean hasHammer = false;
		for(int i = 0; i < 9;i++)
		{
			if(entityplayer.inventory.mainInventory[i] != null && entityplayer.inventory.mainInventory[i].getItem() instanceof ItemHammer)
				hasHammer = true;
		}
		if(entityplayer.getCurrentEquippedItem() != null && entityplayer.getCurrentEquippedItem().getItem() instanceof ItemChisel && hasHammer && !world.isRemote)
		{
			int id = world.getBlockId(x, y, z);
			byte meta = (byte) world.getBlockMetadata(x, y, z);

			int mode = 0;
			PlayerInfo pi = PlayerManagerTFC.getInstance().getPlayerInfoFromPlayer(entityplayer);
			if(pi!=null) mode = pi.ChiselMode;

			if(mode == 3 && xSelected != -10)
			{
				//ItemChisel.CreateDetailed(world, x, y, z, id, meta, side, hitX, hitY, hitZ);
				TileEntityDetailed te = (TileEntityDetailed) world.getBlockTileEntity(x, y, z);
				int index = (xSelected * 8 + zSelected)*8 + ySelected;

				if(index >= 0 && !te.isFinished)
				{
					System.out.println("xSelected: " +xSelected + " ySelected: " + ySelected + " zSelected: " + zSelected + " index: " + index);
					te.data.clear(index);
					te.setEdited();
					te.broadcastPacketInRange(te.createUpdatePacket(index));
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void addCollidingBlockToList(World world, int i, int j, int k, AxisAlignedBB aabb, List list, Entity entity)
	{
		TileEntityDetailed te = (TileEntityDetailed) world.getBlockTileEntity(i, j, k);
		float div = 1f / 8;

		for(int subX = 0; subX < 8; subX++)
		{
			for(int subZ = 0; subZ < 8; subZ++)
			{
				for(int subY = 0; subY < 8; subY++)
				{
					if (te.data.get((subX * 8 + subZ)*8 + subY))
					{
						float minX = subX * div;
						float maxX = minX + div;
						float minY = subY * div;
						float maxY = minY + div;
						float minZ = subZ * div;
						float maxZ = minZ + div;

						this.setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
						super.addCollidingBlockToList(world, i, j, k, aabb, list, entity);
					}
				}
			}
		}
		setBlockBoundsBasedOnSelection(world, i, j, k);
	}


	public int xSelected = -10, ySelected = -10, zSelected = -10, side = -1;

	@Override
	public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 player, Vec3 view) {

		TileEntityDetailed te = (TileEntityDetailed) world.getBlockTileEntity(x, y, z);

		player = player.addVector(-x, -y, -z);
		view = view.addVector(-x, -y, -z);
		if (te == null) {
			return null;
		}

		List<Object[]> returns = new ArrayList<Object[]>();

		//Creates all possible collisions and returns any that collide
		returns = CollisionRayTraceDetailed.rayTraceSubBlocks(
				this,
				player,
				view,
				x,
				y,
				z,
				returns,
				te.data,
				te);
		if(te.isFinished)
		{
			//Check if the block itself is beign collided with
			returns = CollisionRayTraceDetailed.collisionRayTracer(
					this,
					world,
					player,
					view,
					x,
					y,
					z,
					returns);
		}

		if (!returns.isEmpty()) {
			Object[] min = null;
			double distMin = 0;
			for (Object[] ret : returns) 
			{
				double dist = (Double) ret[2];
				if (min == null || dist < distMin) 
				{
					distMin = dist;
					min = ret;
				}
			}
			if (min != null) {
				side = (Byte) min[1];
				xSelected = (Integer) min[3];
				ySelected = (Integer) min[4];
				zSelected = (Integer) min[5];

				int index = (xSelected * 8 + zSelected)*8 + ySelected;

				if (index >= 0 && te.data.get(index)) 
				{
					int d = TileEntityWoodConstruct.PlankDetailLevel;
					int dd = d*d;
					float div = 1f / d;

					float minX = x + xSelected * div;
					float maxX = minX + div;
					float minY = y + ySelected * div;
					float maxY = minY + div;
					float minZ = z + zSelected * div;
					float maxZ = minZ + div;

					this.setBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
					rayTraceBound(AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), x, y, z, player, view);
				}
				setBlockBoundsBasedOnSelection(world, x, y, z);

				return new MovingObjectPosition(x,y,z,
						(Byte) min[1],
						((Vec3) min[0]).addVector(x, y, z));
			}
		}
		xSelected = -10;
		ySelected = -10;
		zSelected = -10;
		side = -1;
		setBlockBoundsBasedOnSelection(world, x, y, z);

		return null;
	}

	public void setBlockBoundsBasedOnSelection(IBlockAccess access, int x, int y, int z) 
	{
		if(xSelected == -10)
		{
			setBlockBounds(0f, 0f, 0f, 0f, 0f, 0f);
		}
		else
		{
			TileEntityDetailed te = (TileEntityDetailed) access.getBlockTileEntity(x, y, z);
			int index = (xSelected * 8 + zSelected)*8 + ySelected;

			if(index >= 0 && te.data.get(index))
			{
				int d = 8;
				int dd = d * d;
				int dd2 = dd*2;

				float div = 1f / d;

				float minX = xSelected * div;
				float maxX = minX + div;
				float minY = ySelected * div;
				float maxY = minY + div;
				float minZ = zSelected * div;
				float maxZ = minZ + div;



				AxisAlignedBB bound = AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
				setBlockBounds(
						(float) bound.minX,
						(float) bound.minY,
						(float) bound.minZ,
						(float) bound.maxX,
						(float) bound.maxY,
						(float) bound.maxZ);
			}
		}
	}

	public Object[] rayTraceBound(AxisAlignedBB bound, int i, int j, int k, Vec3 player, Vec3 view) {
		Vec3 minX = player.getIntermediateWithXValue(view, bound.minX);
		Vec3 maxX = player.getIntermediateWithXValue(view, bound.maxX);
		Vec3 minY = player.getIntermediateWithYValue(view, bound.minY);
		Vec3 maxY = player.getIntermediateWithYValue(view, bound.maxY);
		Vec3 minZ = player.getIntermediateWithZValue(view, bound.minZ);
		Vec3 maxZ = player.getIntermediateWithZValue(view, bound.maxZ);
		if (!isVecInsideYZBounds(bound, minX)) {
			minX = null;
		}
		if (!isVecInsideYZBounds(bound, maxX)) {
			maxX = null;
		}
		if (!isVecInsideXZBounds(bound, minY)) {
			minY = null;
		}
		if (!isVecInsideXZBounds(bound, maxY)) {
			maxY = null;
		}
		if (!isVecInsideXYBounds(bound, minZ)) {
			minZ = null;
		}
		if (!isVecInsideXYBounds(bound, maxZ)) {
			maxZ = null;
		}
		Vec3 tracedBound = null;
		if (minX != null && (tracedBound == null || player.distanceTo(minX) < player
				.distanceTo(tracedBound))) {
			tracedBound = minX;
		}
		if (maxX != null && (tracedBound == null || player.distanceTo(maxX) < player
				.distanceTo(tracedBound))) {
			tracedBound = maxX;
		}
		if (minY != null && (tracedBound == null || player.distanceTo(minY) < player
				.distanceTo(tracedBound))) {
			tracedBound = minY;
		}
		if (maxY != null && (tracedBound == null || player.distanceTo(maxY) < player
				.distanceTo(tracedBound))) {
			tracedBound = maxY;
		}
		if (minZ != null && (tracedBound == null || player.distanceTo(minZ) < player
				.distanceTo(tracedBound))) {
			tracedBound = minZ;
		}
		if (maxZ != null && (tracedBound == null || player.distanceTo(maxZ) < player
				.distanceTo(tracedBound))) {
			tracedBound = maxZ;
		}
		if (tracedBound == null) {
			return null;
		}
		byte side = -1;
		if (tracedBound == minX) {
			side = 4;
		}
		if (tracedBound == maxX) {
			side = 5;
		}
		if (tracedBound == minY) {
			side = 0;
		}
		if (tracedBound == maxY) {
			side = 1;
		}
		if (tracedBound == minZ) {
			side = 2;
		}
		if (tracedBound == maxZ) {
			side = 3;
		}
		return new Object[] { tracedBound, side, player.distanceTo(tracedBound) };
	}

	private boolean isVecInsideYZBounds(AxisAlignedBB bound, Vec3 Vec3) {
		if (Vec3 == null) {
			return false;
		} else {
			return Vec3.yCoord >= bound.minY && Vec3.yCoord <= bound.maxY && Vec3.zCoord >= bound.minZ && Vec3.zCoord <= bound.maxZ;
		}
	}

	private boolean isVecInsideXZBounds(AxisAlignedBB bound, Vec3 Vec3) {
		if (Vec3 == null) {
			return false;
		} else {
			return Vec3.xCoord >= bound.minX && Vec3.xCoord <= bound.maxX && Vec3.zCoord >= bound.minZ && Vec3.zCoord <= bound.maxZ;
		}
	}

	private boolean isVecInsideXYBounds(AxisAlignedBB bound, Vec3 Vec3) {
		if (Vec3 == null) {
			return false;
		} else {
			return Vec3.xCoord >= bound.minX && Vec3.xCoord <= bound.maxX && Vec3.yCoord >= bound.minY && Vec3.yCoord <= bound.maxY;
		}
	}

}