package blusunrize.immersiveengineering.client;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IIEMetaBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;

public class IECustomStateMapper extends StateMapperBase
{
	public static IECustomStateMapper instance = new IECustomStateMapper();

	@Override
	protected ModelResourceLocation getModelResourceLocation(IBlockState state)
	{
		IIEMetaBlock metaBlock = (IIEMetaBlock)state.getBlock();
		String name = ((ResourceLocation)Block.blockRegistry.getNameForObject(state.getBlock())).toString();
		String custom = metaBlock.getCustomStateMapping(state.getBlock().getMetaFromState(state));
		String prop = this.getPropertyString(state.getProperties());
		if(custom!=null)
			return new ModelResourceLocation(name+"_"+custom, prop);
		return new ModelResourceLocation(name, prop);
	}
}