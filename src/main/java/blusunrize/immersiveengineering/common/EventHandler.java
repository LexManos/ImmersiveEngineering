package blusunrize.immersiveengineering.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.DimensionBlockPos;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.energy.wires.IICProxy;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.shader.IShaderItem;
import blusunrize.immersiveengineering.api.shader.ShaderCaseMinecart;
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler;
import blusunrize.immersiveengineering.api.tool.ExcavatorHandler.MineralMix;
import blusunrize.immersiveengineering.api.tool.IDrillHead;
import blusunrize.immersiveengineering.common.blocks.BlockIEBase;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.ISpawnInterdiction;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityCrusher;
import blusunrize.immersiveengineering.common.entities.EntityGrapplingHook;
import blusunrize.immersiveengineering.common.entities.EntityPropertiesShaderCart;
import blusunrize.immersiveengineering.common.items.ItemDrill;
import blusunrize.immersiveengineering.common.items.ItemManeuverGear;
import blusunrize.immersiveengineering.common.util.IEAchievements;
import blusunrize.immersiveengineering.common.util.IEExplosion;
import blusunrize.immersiveengineering.common.util.IELogger;
import blusunrize.immersiveengineering.common.util.IEPotions;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import blusunrize.immersiveengineering.common.util.ManeuverGearHelper;
import blusunrize.immersiveengineering.common.util.ManeuverGearHelper.HookMode;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.compat.computercraft.TileEntityRequest;
import blusunrize.immersiveengineering.common.util.network.MessageMinecartShaderSync;
import blusunrize.immersiveengineering.common.util.network.MessageMineralListSync;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.oredict.OreDictionary;

public class EventHandler
{
	public static ArrayList<ISpawnInterdiction> interdictionTiles = new ArrayList<ISpawnInterdiction>();
	public static boolean validateConnsNextTick = false;
	public static HashSet<IEExplosion> currentExplosions = new HashSet<IEExplosion>();
	public static Set<TileEntityRequest> ccRequestedTEs = Collections.newSetFromMap(new ConcurrentHashMap<TileEntityRequest, Boolean>());
	public static Set<TileEntityRequest> cachedRequestResults = Collections.newSetFromMap(new ConcurrentHashMap<TileEntityRequest, Boolean>());
	@SubscribeEvent
	public void onLoad(WorldEvent.Load event)
	{
		if(ImmersiveNetHandler.INSTANCE==null)
			ImmersiveNetHandler.INSTANCE = new ImmersiveNetHandler();
		//		if(event.world.provider.dimensionId==0)
		//		{
		/**
		if(ImmersiveNetHandler.INSTANCE==null)
			ImmersiveNetHandler.INSTANCE = new ImmersiveNetHandler();
		if(!event.world.isRemote && !IESaveData.loaded)
		{
			IELogger.info("[ImEng] - world data loading, dimension "+event.world.provider.dimensionId);
			IESaveData worldData = (IESaveData) event.world.loadItemData(IESaveData.class, IESaveData.dataName);
			if(worldData==null)
			{
				IELogger.info("[ImEng] - No World Data Found");
				worldData = new IESaveData(IESaveData.dataName);
				//				worldData.dimension = event.world.provider.dimensionId;
				event.world.setItemData(IESaveData.dataName, worldData);
			}
			else
				IELogger.info("World Data Retrieved");
			IESaveData.setInstance(event.world.provider.dimensionId, worldData);
			IESaveData.loaded = true;
		}
		 */
		//		}
		ImmersiveEngineering.proxy.onWorldLoad();
	}
	//transferPerTick
	@SubscribeEvent
	public void onSave(WorldEvent.Save event)
	{
		IESaveData.setDirty(0);
	}
	@SubscribeEvent
	public void onUnload(WorldEvent.Unload event)
	{
		IESaveData.setDirty(0);
	}

	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event)
	{
		if(event.entity instanceof EntityMinecart)
		{
			for(Class<? extends EntityMinecart> invalid : ShaderCaseMinecart.invalidMinecartClasses)
				if(invalid.isAssignableFrom(event.entity.getClass())) return;
			event.entity.registerExtendedProperties(EntityPropertiesShaderCart.PROPERTY_NAME, new EntityPropertiesShaderCart());
		}
	}
	@SubscribeEvent
	public void onEntityJoiningWorld(EntityJoinWorldEvent event)
	{
		if(event.entity.worldObj.isRemote && event.entity instanceof EntityMinecart && event.entity.getExtendedProperties(EntityPropertiesShaderCart.PROPERTY_NAME)!=null)
			ImmersiveEngineering.packetHandler.sendToServer(new MessageMinecartShaderSync(event.entity,null));
	}
	@SubscribeEvent
	public void onEntityInteract(EntityInteractEvent event)
	{
		if(event.target instanceof EntityLivingBase && OreDictionary.itemMatches(new ItemStack(IEContent.itemRevolver,1,OreDictionary.WILDCARD_VALUE), event.entityPlayer.getCurrentEquippedItem(), false))
			event.setCanceled(true);
		if(!event.entityPlayer.worldObj.isRemote && event.target instanceof EntityMinecart && event.entityPlayer.getCurrentEquippedItem()!=null && event.entityPlayer.getCurrentEquippedItem().getItem() instanceof IShaderItem)
		{
			EntityPropertiesShaderCart properties = (EntityPropertiesShaderCart)event.target.getExtendedProperties(EntityPropertiesShaderCart.PROPERTY_NAME);
			if(properties!=null)
			{
				properties.setShader(Utils.copyStackWithAmount(event.entityPlayer.getCurrentEquippedItem(), 1));
				ImmersiveEngineering.packetHandler.sendTo(new MessageMinecartShaderSync(event.target,properties), (EntityPlayerMP)event.entityPlayer);
				event.setCanceled(true);
			}
		}
	}



	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event)
	{
		if (event.phase==TickEvent.Phase.START && validateConnsNextTick && FMLCommonHandler.instance().getEffectiveSide()==Side.SERVER)
		{
			boolean validateConnections = Config.getBoolean("validateConnections");
			int invalidConnectionsDropped = 0;
			for (int dim:ImmersiveNetHandler.INSTANCE.getRelevantDimensions())
			{
				World world = MinecraftServer.getServer().worldServerForDimension(dim);
				if (world==null) {
					ImmersiveNetHandler.INSTANCE.directConnections.remove(dim);
					continue;
				}
				if (validateConnections)
				{
					for (Connection con:ImmersiveNetHandler.INSTANCE.getAllConnections(world))
					{
						if (!(world.getTileEntity(con.start) instanceof IImmersiveConnectable
								&& world.getTileEntity(con.end) instanceof IImmersiveConnectable))
						{
							ImmersiveNetHandler.INSTANCE.removeConnection(world, con);
							invalidConnectionsDropped++;
						}
					}
					IELogger.info("removed "+invalidConnectionsDropped+" invalid connections from world");
				}
			}
			int invalidProxies = 0;
			Set<DimensionBlockPos> toRemove = new HashSet<>();
			for (Entry<DimensionBlockPos, IICProxy> e:ImmersiveNetHandler.INSTANCE.proxies.entrySet())
			{
				DimensionBlockPos p = e.getKey();
				World w = MinecraftServer.getServer().worldServerForDimension(p.dimension);
				if (w!=null&&w.isBlockLoaded(p))
					toRemove.add(p);
				if (validateConnections&&w==null)
				{
					invalidProxies++;
					toRemove.add(p);
					continue;
				}
				if (validateConnections&&!(w.getTileEntity(p) instanceof IImmersiveConnectable))
				{
					invalidProxies++;
					toRemove.add(p);
				}
			}
			if (invalidProxies>0)
				IELogger.info("Removed "+invalidProxies+" invalid connector proxies (used to transfer power through unloaded chunks)");
			validateConnsNextTick = false;
		}
		if(event.phase==TickEvent.Phase.END && FMLCommonHandler.instance().getEffectiveSide()==Side.SERVER)
		{
			for(Map.Entry<Connection, Integer> e : ImmersiveNetHandler.INSTANCE.getTransferedRates(event.world.provider.getDimensionId()).entrySet())
				if(e.getValue()>e.getKey().cableType.getTransferRate())
				{
					if(event.world instanceof WorldServer)
						for(Vec3 vec : e.getKey().getSubVertices(event.world))
							((WorldServer)event.world).spawnParticle(EnumParticleTypes.FLAME, false, vec.xCoord,vec.yCoord,vec.zCoord, 0, 0,.02,0, 1, new int[0]);
					ImmersiveNetHandler.INSTANCE.removeConnection(event.world, e.getKey());
				}
			ImmersiveNetHandler.INSTANCE.getTransferedRates(event.world.provider.getDimensionId()).clear();
			// CC tile entity requests
			Iterator<TileEntityRequest> it;
			it = cachedRequestResults.iterator();
			while (it.hasNext())
			{
				TileEntityRequest req = it.next();
				if (!ccRequestedTEs.contains(req))
				{
					it.remove();
					continue;
				}
				req.te = req.w.getTileEntity(req.pos);
			}
			it = ccRequestedTEs.iterator();
			int timeout = 100;
			while (it.hasNext() && timeout > 0)
			{
				TileEntityRequest req = it.next();
				synchronized (req)
				{
					req.te = req.w.getTileEntity(req.pos);
					req.checked = true;
					req.notifyAll();
				}
				it.remove();
				timeout--;
				cachedRequestResults.add(req);
			}
		}
		if(event.phase==TickEvent.Phase.START)
		{
			if(!currentExplosions.isEmpty())
			{
				Iterator<IEExplosion> itExplosion = currentExplosions.iterator();
				while(itExplosion.hasNext())
				{
					IEExplosion ex = itExplosion.next();
					ex.doExplosionTick();
					if(ex.isExplosionFinished)
						itExplosion.remove();
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event)
	{
		if(ManeuverGearHelper.isPlayerWearing3DMG(event.player))
		{
			EntityGrapplingHook[] hooks = ManeuverGearHelper.getHooks(event.player);
			Vec3 newMotion = new Vec3(0,0,0);
			int vec = 0;
			for(int i=0; i<2; i++)
				if(hooks[i]!=null && hooks[i].getHookMode()==HookMode.REELING)
				{
					double speed = hooks[i].getHookSpeed();
					Vec3 hookMotion = new Vec3((hooks[i].posX-event.player.posX)*speed, (hooks[i].posY-(event.player.posY+event.player.height/2))*speed, (hooks[i].posZ-event.player.posZ)*speed).normalize();
					newMotion = newMotion.add(hookMotion);
					vec++;
				}
			if(vec>0)
			{
				event.player.motionX = newMotion.xCoord/vec;
				event.player.motionY = newMotion.yCoord/vec;
				event.player.motionZ = newMotion.zCoord/vec;
				event.player.fallDistance = 0;
			}
		}
	}

	@SubscribeEvent
	public void onLogin(PlayerLoggedInEvent event)
	{
		if(!event.player.worldObj.isRemote)
		{
			HashMap<MineralMix,Integer> packetMap = new HashMap<MineralMix,Integer>(); 
			for(Map.Entry<MineralMix,Integer> e: ExcavatorHandler.mineralList.entrySet())
				if(e.getKey()!=null && e.getValue()!=null)
					packetMap.put(e.getKey(), e.getValue());
			ImmersiveEngineering.packetHandler.sendToAll(new MessageMineralListSync(packetMap));
		}
	}

	@SubscribeEvent
	public void harvestCheck(PlayerEvent.HarvestCheck event)
	{
		if(event.block instanceof BlockIEBase && event.entityPlayer.getCurrentEquippedItem()!=null && event.entityPlayer.getCurrentEquippedItem().getItem().getToolClasses(event.entityPlayer.getCurrentEquippedItem()).contains(Lib.TOOL_HAMMER))
		{
			MovingObjectPosition mop = Utils.getMovingObjectPositionFromPlayer(event.entityPlayer.worldObj, event.entityPlayer, true);
			if(mop!=null && mop.typeOfHit==MovingObjectPosition.MovingObjectType.BLOCK)
				if(((BlockIEBase)event.block).allowHammerHarvest(event.entityPlayer.worldObj.getBlockState(mop.getBlockPos())))
					event.success=true;
		}

	}
	//	@SubscribeEvent
	//	public void bloodMagicTeleposer(TeleposeEvent event)
	//	{
	//		TileEntity tI = event.initialWorld.getTileEntity(event.initialX, event.initialY, event.initialZ);
	//		TileEntity tF = event.finalWorld.getTileEntity(event.finalX, event.finalY, event.finalZ);
	//		if(tI instanceof TileEntityImmersiveConnectable || tF instanceof TileEntityImmersiveConnectable)
	//			event.setCanceled(true);
	//		if(tI instanceof TileEntityMultiblockPart || tF instanceof TileEntityMultiblockPart)
	//			event.setCanceled(true);
	//	}

	public static HashMap<UUID, TileEntityCrusher> crusherMap = new HashMap<UUID, TileEntityCrusher>();
	public static HashSet<Class<? extends EntityLiving>> listOfBoringBosses = new HashSet();
	static{
		listOfBoringBosses.add(EntityWither.class);
	}
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onLivingDropsLowest(LivingDropsEvent event)
	{
		if(!event.isCanceled() && Lib.DMG_Crusher.equals(event.source.getDamageType()))
		{
			//TODO Reenable with Crusher!
			TileEntityCrusher crusher = crusherMap.get(event.entityLiving.getUniqueID());
			if(crusher!=null)
			{
				for(EntityItem item: event.drops)
					if(item!=null && item.getEntityItem()!=null)
						crusher.doProcessOutput(item.getEntityItem());
				crusherMap.remove(event.entityLiving.getUniqueID());
				event.setCanceled(true);
			}
		}
	}
	@SubscribeEvent
	public void onLivingDrops(LivingDropsEvent event)
	{
		if(!event.isCanceled() && event.entityLiving instanceof IBossDisplayData)
		{
			EnumRarity r = EnumRarity.EPIC;
			for(Class<? extends EntityLiving> boring : listOfBoringBosses)
				if(boring.isAssignableFrom(event.entityLiving.getClass()))
				{
					r = EnumRarity.RARE;
					break;
				}
			ItemStack bag = new ItemStack(IEContent.itemShaderBag);
			ItemNBTHelper.setString(bag, "rarity", r.toString());
			event.drops.add(new EntityItem(event.entityLiving.worldObj, event.entityLiving.posX,event.entityLiving.posY,event.entityLiving.posZ, bag));
		}
	}

	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onLivingHurt(LivingHurtEvent event)
	{
		if(event.source.isFireDamage() && event.entityLiving.getActivePotionEffect(IEPotions.flammable)!=null)
		{
			int amp = event.entityLiving.getActivePotionEffect(IEPotions.flammable).getAmplifier();
			float mod = 1.5f + ((amp*amp)*.5f);
			event.ammount *= mod;
		}
		if(event.source.getDamageType().equals("flux") && event.entityLiving.getActivePotionEffect(IEPotions.conductive)!=null)
		{
			int amp = event.entityLiving.getActivePotionEffect(IEPotions.conductive).getAmplifier();
			float mod = 1.5f + ((amp*amp)*.5f); 
			event.ammount *= mod;
		}
		if(event.entityLiving instanceof EntityPlayer && event.source==DamageSource.fall && ManeuverGearHelper.isPlayerWearing3DMG((EntityPlayer)event.entityLiving))
		{
			ItemStack gear = ManeuverGearHelper.getPlayer3DMG((EntityPlayer)event.entityLiving);
			if(gear!=null)
			{
				float gas = ItemNBTHelper.getFloat(gear, "gas");
				int reduce = (int)Math.min(event.ammount, Math.floor(gas/ItemManeuverGear.jumpCost));
				if(reduce>0)
				{
					event.ammount -= reduce;
					gas -= reduce*ItemManeuverGear.jumpCost;
					ItemNBTHelper.setFloat(gear, "gas", gas);
					ItemNBTHelper.setInt(gear, "cooldown", ItemManeuverGear.rechargeCooldown);
					ManeuverGearHelper.updatePlayer3DMG((EntityPlayer)event.entityLiving, gear);
				}
			}
		}
	}
	@SubscribeEvent
	public void onLivingJump(LivingJumpEvent event)
	{
		if(event.entityLiving.getActivePotionEffect(IEPotions.sticky)!=null)
			event.entityLiving.motionY -= (event.entityLiving.getActivePotionEffect(IEPotions.sticky).getAmplifier()+1)*0.3F;
	}

	@SubscribeEvent
	public void onEnderTeleport(EnderTeleportEvent event)
	{
		if(event.entityLiving.isCreatureType(EnumCreatureType.MONSTER, false))
		{
			synchronized (interdictionTiles) {
				Iterator<ISpawnInterdiction> it = interdictionTiles.iterator();
				while(it.hasNext())
				{
					ISpawnInterdiction interdictor = it.next();
					if(interdictor instanceof TileEntity)
					{
						if(((TileEntity)interdictor).isInvalid() || ((TileEntity)interdictor).getWorld()==null)
							it.remove();
						else if( ((TileEntity)interdictor).getWorld().provider.getDimensionId()==event.entity.worldObj.provider.getDimensionId() && ((TileEntity)interdictor).getDistanceSq(event.entity.posX, event.entity.posY, event.entity.posZ)<=interdictor.getInterdictionRangeSquared())
							event.setCanceled(true);
					}
					else if(interdictor instanceof Entity)
					{
						if(((Entity)interdictor).isDead || ((Entity)interdictor).worldObj==null)
							it.remove();
						else if(((Entity)interdictor).worldObj.provider.getDimensionId()==event.entity.worldObj.provider.getDimensionId() && ((Entity)interdictor).getDistanceSqToEntity(event.entity)<=interdictor.getInterdictionRangeSquared())
							event.setCanceled(true);
					}
				}
			}
		}
		if(event.entityLiving.getActivePotionEffect(IEPotions.stunned)!=null)
			event.setCanceled(true);
	}
	@SubscribeEvent
	public void onEntitySpawnCheck(LivingSpawnEvent.CheckSpawn event)
	{
		if(event.getResult() == Event.Result.ALLOW||event.getResult() == Event.Result.DENY)
			return;
		if(event.entityLiving.isCreatureType(EnumCreatureType.MONSTER, false))
		{
			synchronized (interdictionTiles) {
				Iterator<ISpawnInterdiction> it = interdictionTiles.iterator();
				while(it.hasNext())
				{
					ISpawnInterdiction interdictor = it.next();
					if(interdictor instanceof TileEntity)
					{
						if(((TileEntity)interdictor).isInvalid() || ((TileEntity)interdictor).getWorld()==null)
							it.remove();
						else if( ((TileEntity)interdictor).getWorld().provider.getDimensionId()==event.entity.worldObj.provider.getDimensionId() && ((TileEntity)interdictor).getDistanceSq(event.entity.posX, event.entity.posY, event.entity.posZ)<=interdictor.getInterdictionRangeSquared())
							event.setResult(Event.Result.DENY);
					}
					else if(interdictor instanceof Entity)
					{
						if(((Entity)interdictor).isDead || ((Entity)interdictor).worldObj==null)
							it.remove();
						else if(((Entity)interdictor).worldObj.provider.getDimensionId()==event.entity.worldObj.provider.getDimensionId() && ((Entity)interdictor).getDistanceSqToEntity(event.entity)<=interdictor.getInterdictionRangeSquared())
							event.setResult(Event.Result.DENY);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onItemCrafted(ItemCraftedEvent event)
	{
		if(event.player!=null)
			for(IEAchievements.AchievementIE achievement : IEAchievements.normalCraftingAchievements)
			{
				if(achievement.triggerItems!=null && achievement.triggerItems.length>0)
				{
					for(ItemStack trigger : achievement.triggerItems)
						if(OreDictionary.itemMatches(trigger, event.crafting, true))
						{
							event.player.triggerAchievement(achievement);
							break;
						}
				}
				else if(OreDictionary.itemMatches(achievement.theItemStack, event.crafting, true))
					event.player.triggerAchievement(achievement);
			}

		if(event.crafting!=null && ItemNBTHelper.hasKey(event.crafting, "jerrycanFilling"))
		{
			int drain = ItemNBTHelper.getInt(event.crafting, "jerrycanFilling");
			for(int i=0;i<event.craftMatrix.getSizeInventory();i++)
			{
				ItemStack stackInSlot = event.craftMatrix.getStackInSlot(i);
				if(stackInSlot!=null)
					if(IEContent.itemJerrycan.equals(stackInSlot.getItem()) && ItemNBTHelper.hasKey(stackInSlot, "fluid"))
					{
						ItemNBTHelper.setInt(stackInSlot, "jerrycanDrain", drain);
						break;
					}
			}
			ItemNBTHelper.remove(event.crafting, "jerrycanFilling");	
		}
	}

	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onBlockPlaced(BlockEvent.PlaceEvent event)
	{
		if(event.player!=null && !event.isCanceled())
			for(IEAchievements.AchievementIE achievement : IEAchievements.placementAchievements)
			{
				if(achievement.triggerItems!=null && achievement.triggerItems.length>0)
				{
					for(ItemStack trigger : achievement.triggerItems)
						if(OreDictionary.itemMatches(trigger, event.itemInHand, true))
						{
							event.player.triggerAchievement(achievement);
							break;
						}
				}
				else if(OreDictionary.itemMatches(achievement.theItemStack, event.itemInHand, true))
					event.player.triggerAchievement(achievement);
			}
	}

	@SubscribeEvent()
	public void digSpeedEvent(PlayerEvent.BreakSpeed event)
	{
		ItemStack current = event.entityPlayer.getCurrentEquippedItem();
		//Stop the combustion drill from working underwater
		if(current!=null && current.getItem().equals(IEContent.itemDrill) && current.getItemDamage()==0 && event.entityPlayer.isInsideOfMaterial(Material.water))
			if( ((ItemDrill)IEContent.itemDrill).getUpgrades(current).getBoolean("waterproof"))
				event.newSpeed*=5;
			else
				event.setCanceled(true);
	}
	@SubscribeEvent
	public void onAnvilChange(AnvilUpdateEvent event)
	{
		if(event.left!=null && event.left.getItem() instanceof IDrillHead && ((IDrillHead)event.left.getItem()).getHeadDamage(event.left)>0)
		{
			if(event.right!=null && event.left.getItem().getIsRepairable(event.left, event.right))
			{
				event.output = event.left.copy();
				int repair = Math.min(
						((IDrillHead)event.output.getItem()).getHeadDamage(event.output),
						((IDrillHead)event.output.getItem()).getMaximumHeadDamage(event.output)/4);
				int cost = 0;
				for(;repair>0&&cost<event.right.stackSize; ++cost)
				{
					((IDrillHead)event.output.getItem()).damageHead(event.output, -repair);
					event.cost += Math.max(1, repair/200);
					repair = Math.min(
							((IDrillHead)event.output.getItem()).getHeadDamage(event.output),
							((IDrillHead)event.output.getItem()).getMaximumHeadDamage(event.output)/4);
				}
				event.materialCost = cost;

				if(event.name==null || event.name.isEmpty())
				{
					if(event.left.hasDisplayName())
					{
						event.cost += 5;
						event.output.clearCustomName();
					}
				}
				else if (!event.name.equals(event.left.getDisplayName()))
				{
					event.cost += 5;
					if(event.left.hasDisplayName())
						event.cost += 2;
					event.output.setStackDisplayName(event.name);
				}
			}
		}
	}
	public static TileEntity requestTE(World w, BlockPos pos)
	{
		TileEntityRequest req = new TileEntityRequest(w, pos);
		TileEntity te = null;
		Iterator<TileEntityRequest> it = cachedRequestResults.iterator();
		while (it.hasNext())
		{
			TileEntityRequest curr = it.next();
			if (req.equals(curr))
				te = curr.te;
		}
		if (te!=null)
			return te;
		synchronized (req)
		{
			ccRequestedTEs.add(req);
			int timeout = 100;
			while (!req.checked&&timeout>0)
			{
				// i don't really know why this is necessary, but the requests sometimes time out without this
				if (!ccRequestedTEs.contains(req))
					ccRequestedTEs.add(req);
				try
				{
					req.wait(50);
				}
				catch (InterruptedException e)
				{}
				timeout--;
			}
			if (!req.checked)
			{
				IELogger.info("Timeout while requesting a TileEntity");
				return w.getTileEntity(pos);
			}
		}
		return req.te;
	}
}