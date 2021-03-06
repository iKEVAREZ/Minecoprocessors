/*
 * Code and resources for our Code Book were taken from TIS-3D
 * (https://github.com/MightyPirates/TIS-3D), released under the MIT license
 * by Florian "Sangar" Nücke.
 */

package net.torocraft.minecoprocessors.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import net.torocraft.minecoprocessors.Minecoprocessors;
import net.torocraft.minecoprocessors.blocks.BlockMinecoprocessor;
import net.torocraft.minecoprocessors.gui.MinecoprocessorGuiHandler;

/**
 * The code book, utility book for coding ASM programs for execution modules.
 */
@Mod.EventBusSubscriber
public final class ItemBookCode extends ItemBook {

  public static final Pattern PATTERN_LINES = Pattern.compile("\r?\n");
  public static final int MAX_LINES_PER_PAGE = 20;

  public static final String NAME = "book_code";
  private static final ResourceLocation REGISTRY_NAME = new ResourceLocation(Minecoprocessors.MODID, NAME);

  public static final ItemBookCode INSTANCE = new ItemBookCode();

  public static boolean isBookCode(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() == INSTANCE;
  }

  @SubscribeEvent
  public static void init(final RegistryEvent.Register<Item> event) {
    event.getRegistry().register(INSTANCE);
    OreDictionary.registerOre("book", ItemBookCode.INSTANCE);
  }

  @SubscribeEvent
  public static void registerModels(@SuppressWarnings("unused") ModelRegistryEvent event) {
    ModelLoader.setCustomModelResourceLocation(INSTANCE, 0, new ModelResourceLocation(REGISTRY_NAME, "inventory"));
  }

  @SubscribeEvent
  public static void registerRecipes(final RegistryEvent.Register<IRecipe> event) {
    NonNullList<Ingredient> lst = NonNullList.create();
    lst.add(Ingredient.fromItem(Items.WRITABLE_BOOK));
    lst.add(Ingredient.fromItem(BlockMinecoprocessor.ITEM_INSTANCE));
    event.getRegistry().register(new ShapelessRecipes("", new ItemStack(ItemBookCode.INSTANCE), lst) {
      @Override
      public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        NonNullList<ItemStack> l = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < l.size(); ++i) {
          ItemStack stack = inv.getStackInSlot(i);

          if (stack.getItem() == BlockMinecoprocessor.ITEM_INSTANCE) {
            ItemStack returnStack = stack.copy();
            returnStack.setCount(1);
            l.set(i, returnStack);
            return l;
          }
        }

        throw new RuntimeException("Item to return not found in inventory");
      }
    }.setRegistryName(ItemBookCode.REGISTRY_NAME));
  }

  protected ItemBookCode() {
    setMaxStackSize(1);
    setCreativeTab(CreativeTabs.REDSTONE);
    setUnlocalizedName(NAME);
    setRegistryName(REGISTRY_NAME);
  }

  // --------------------------------------------------------------------- //
  // Item

  @Override
  public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
    if (world.isRemote) {
      player.openGui(Minecoprocessors.INSTANCE, MinecoprocessorGuiHandler.MINECOPROCESSOR_BOOK_GUI, world, 0, 0, 0);
    }
    return super.onItemRightClick(world, player, hand);
  }

  // --------------------------------------------------------------------- //
  // ItemBook

  @Override
  public boolean isEnchantable(final ItemStack stack) {
    return false;
  }

  @Override
  public int getItemEnchantability() {
    return 0;
  }

  @SideOnly(Side.CLIENT)
  @Override
  public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
    tooltip.add(Minecoprocessors.proxy.i18nFormat("item.book_code.tooltip"));
  }

  // --------------------------------------------------------------------- //

  /**
   * Wrapper for list of pages stored in the code book.
   */
  public static class Data {

    //public static final String CONTINUATION_MACRO = "#BWTM";
    private static final String TAG_PAGES = "pages";
    private static final String TAG_SELECTED = "selected";

    private final List<List<String>> pages = new ArrayList<>();
    private int selectedPage = 0;

    // --------------------------------------------------------------------- //

    /**
     * Get the page currently selected in the book.
     *
     * @return the index of the selected page.
     */
    public int getSelectedPage() {
      return selectedPage;
    }

    /**
     * Set which page is currently selected.
     *
     * @param index the new selected index.
     */
    public void setSelectedPage(final int index) {
      this.selectedPage = index;
      validateSelectedPage();
    }

    /**
     * Get the number of pages stored in the book.
     *
     * @return the number of pages stored in the book.
     */
    public int getPageCount() {
      return pages.size();
    }

    /**
     * Get the code on the specified page.
     *
     * @param index the index of the page to get the code of.
     * @return the code on the page.
     */
    public List<String> getPage(final int index) {
      return Collections.unmodifiableList(pages.get(index));
    }

    /**
     * Add a new, blank page to the book.
     */
    public void addPage() {
      pages.add(Collections.singletonList(""));
      setSelectedPage(pages.size() - 1);
    }

    /**
     * Overwrite a page at the specified index.
     *
     * @param page the index of the page to overwrite.
     * @param code the code of the page.
     */
    public void setPage(final int page, final List<String> code) {
      pages.set(page, new ArrayList<>(code));
    }

    /**
     * Remove a page from the book.
     *
     * @param index the index of the page to remove.
     */
    public void removePage(final int index) {
      pages.remove(index);
      validateSelectedPage();
    }

    /**
     * Get the complete program of the selected page, taking into account the <code>#BWTM</code> preprocessor macro allowing programs to span multiple pages.
     *
     * @return the full program starting on the current page.
     */
    public List<List<String>> getProgram() {
      return Collections.unmodifiableList(pages);
    }

    public List<String> getContinuousProgram() {
      final List<String> program = new ArrayList<>();
      for (int i = 0; i < pages.size(); i++) {
        program.addAll(getPage(i));
      }
      return program;
    }

    /**
     * Load data from the specified NBT tag.
     *
     * @param nbt the tag to load the data from.
     */
    public void readFromNBT(final NBTTagCompound nbt) {
      pages.clear();

      final NBTTagList pagesNbt = nbt.getTagList(TAG_PAGES, net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
      for (int index = 0; index < pagesNbt.tagCount(); index++) {
        pages.add(Arrays.asList(PATTERN_LINES.split(pagesNbt.getStringTagAt(index))));
      }

      selectedPage = nbt.getInteger(TAG_SELECTED);
      validateSelectedPage();
    }

    /**
     * Store the data to the specified NBT tag.
     *
     * @param nbt the tag to save the data to.
     */
    public void writeToNBT(final NBTTagCompound nbt) {
      final NBTTagList pagesNbt = new NBTTagList();
      int removed = 0;
      for (int index = 0; index < pages.size(); index++) {
        final List<String> program = pages.get(index);
        if (program.size() > 1 || program.get(0).length() > 0) {
          pagesNbt.appendTag(new NBTTagString(String.join("\n", program)));
        } else if (index < selectedPage) {
          removed++;
        }
      }
      nbt.setTag(TAG_PAGES, pagesNbt);

      nbt.setInteger(TAG_SELECTED, selectedPage - removed);
    }

    // --------------------------------------------------------------------- //

    private void validateSelectedPage() {
      selectedPage = Math.max(0, Math.min(pages.size() - 1, selectedPage));
    }

    private boolean areAllPagesEqual(final List<List<String>> newPages, final int startPage) {
      for (int offset = 0; offset < newPages.size(); offset++) {
        final List<String> have = pages.get(startPage + offset);
        final List<String> want = newPages.get(offset);
        if (!Objects.equals(have, want)) {
          return false;
        }
      }

      return true;
    }

    // --------------------------------------------------------------------- //

    /**
     * Load code book data from the specified NBT tag.
     *
     * @param nbt the tag to load the data from.
     * @return the data loaded from the tag.
     */
    public static Data loadFromNBT(@Nullable final NBTTagCompound nbt) {
      final Data data = new Data();
      if (nbt != null) {
        data.readFromNBT(nbt);
      }
      return data;
    }

    /**
     * Load code book data from the specified item stack.
     *
     * @param stack the item stack to load the data from.
     * @return the data loaded from the stack.
     */
    public static Data loadFromStack(final ItemStack stack) {
      return loadFromNBT(stack.getTagCompound());
    }

    /**
     * Save the specified code book data to the specified item stack.
     *
     * @param stack the item stack to save the data to.
     * @param data the data to save to the item stack.
     */
    public static void saveToStack(final ItemStack stack, final Data data) {
      NBTTagCompound nbt = stack.getTagCompound();
      if (nbt == null) {
        stack.setTagCompound(nbt = new NBTTagCompound());
      }
      data.writeToNBT(nbt);
    }
  }
}
