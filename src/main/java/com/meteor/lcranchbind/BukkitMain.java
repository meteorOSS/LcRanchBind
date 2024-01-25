package com.meteor.lcranchbind;

import catserver.api.bukkit.event.ForgeEvent;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.blocks.BlockProperties;
import com.pixelmonmod.pixelmon.blocks.MultiBlock;
import com.pixelmonmod.pixelmon.blocks.enums.EnumMultiPos;
import com.pixelmonmod.pixelmon.blocks.ranch.BlockRanchBlock;
import com.pixelmonmod.pixelmon.blocks.tileEntities.TileEntityRanchBlock;
import com.pixelmonmod.pixelmon.util.helpers.BlockHelper;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.pixelmonmod.pixelmon.blocks.MultiBlock.MULTIPOS;

public class BukkitMain extends JavaPlugin implements Listener {
    private final String blockName = "PIXELMON_RANCH";

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private String mapperLocation(Location location){
        return location.getWorld().getName()+"#"+location.getBlockX()+"#"+location.getBlockY()+"#"+location.getBlockZ();
    }

    private void saveBlock(ItemStack itemStack, Location location){
        itemStack.setAmount(1);
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("item",itemStack);
        Bukkit.getScheduler().runTaskAsynchronously(this,()->{
            try {
                yamlConfiguration.save(new File(getDataFolder()+"/data/"+mapperLocation(location)+".yml"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ItemStack getItem(Location location){
        File file = new File(getDataFolder() + "/data/" + mapperLocation(location) + ".yml");
        if(file.exists()){
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            ItemStack itemStack = yamlConfiguration.getItemStack("item");
            file.delete();
            return itemStack;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onBreak(BlockBreakEvent blockBreakEvent){
        if(blockBreakEvent.isCancelled()) return;
        Block block = blockBreakEvent.getBlock();
        if(block.getType().toString().equalsIgnoreCase(blockName)){
            ItemStack item = getItem(block.getLocation());
            blockBreakEvent.setCancelled(true);
            Location location = block.getLocation();
            BlockPos blockPos = new BlockPos(location.getBlockX(),location.getBlockY(),location.getBlockZ());
            Player player = blockBreakEvent.getPlayer();
            EntityPlayerMP entityPlayerMP = Pixelmon.storageManager.getParty(player.getUniqueId()).getPlayer();
            World entityWorld = entityPlayerMP.getEntityWorld();
            IBlockState blockState = entityWorld.getBlockState(blockPos);
            if(item!=null){
                BlockRanchBlock blockRanchBlock = (BlockRanchBlock) blockState.getBlock();
                EnumMultiPos value = blockState.getValue(MULTIPOS);
                BlockPos baseBlock = blockRanchBlock.findBaseBlock(entityWorld, new BlockPos.MutableBlockPos(blockPos), entityWorld.getBlockState(blockPos));
                if(baseBlock.toLong()!=blockPos.toLong()){
                    IBlockState blockState1 = entityWorld.getBlockState(baseBlock);
                    value = blockState1.getValue(MULTIPOS);
                }
                Class<MultiBlock> multiBlockClass = MultiBlock.class;
                try {
                    Method method = multiBlockClass.getDeclaredMethod("setMultiBlocksWidth", BlockPos.class, World.class, IBlockState.class);
                    method.setAccessible(true);
                    method.invoke(blockRanchBlock,baseBlock,entityWorld,blockState);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                
                TileEntityRanchBlock ranchblock = BlockHelper.getTileEntity(TileEntityRanchBlock.class, entityWorld,
                        blockRanchBlock.findBaseBlock(entityWorld, new BlockPos.MutableBlockPos(blockPos), entityWorld.getBlockState(blockPos)));
                if (ranchblock != null) {
                    ranchblock.onDestroy();
                }
                entityWorld.setBlockState(blockPos, net.minecraft.init.Blocks.AIR.getDefaultState(), 11);
                player.getWorld().dropItem(block.getLocation(),item);
            }else {
                blockState.getBlock().removedByPlayer(blockState,entityWorld,blockPos,entityPlayerMP,true);
            }

        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    void onBlockPlace(BlockPlaceEvent placeEvent){
        if(placeEvent.isCancelled()) return;
        ItemStack itemInHand = placeEvent.getItemInHand();
        Block block = placeEvent.getBlock();
        if(itemInHand.getType().toString().equalsIgnoreCase(blockName)){
            saveBlock(itemInHand.clone(),block.getLocation());
        }
    }


}
