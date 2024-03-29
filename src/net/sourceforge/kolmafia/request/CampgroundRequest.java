/**
 * Copyright (c) 2005-2015, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CampgroundRequest
	extends GenericRequest
{
	private static final Pattern LIBRAM_PATTERN =
		Pattern.compile( "Summon (Candy Heart|Party Favor|Love Song|BRICKOs|Dice|Resolutions|Taffy) *.[(]([\\d,]+) MP[)]" );
	private static final Pattern HOUSING_PATTERN =
		Pattern.compile( "/rest(\\d+|a|b|c|d|e)(tp)?(_free)?.gif" );
	private static final Pattern FURNISHING_PATTERN =
		Pattern.compile( "<b>(?:an? )?(.*?)</b>" );

	private static final Pattern JUNG_PATTERN = Pattern.compile( "junggate_(\\d)" );
	private static final Pattern DNA_PATTERN = Pattern.compile( "sample of <b>(.*?)</b> DNA" );

	private static int currentDwellingLevel = 0;
	private static AdventureResult currentDwelling = null;
	private static AdventureResult currentBed = null;
	private static AdventureResult currentWorkshedItem = null;

	public static final AdventureResult BIG_ROCK = ItemPool.get( ItemPool.BIG_ROCK, 1 );

	public static final AdventureResult BLACK_BLUE_LIGHT = ItemPool.get( ItemPool.BLACK_BLUE_LIGHT, 1 );
	public static final AdventureResult LOUDMOUTH_LARRY = ItemPool.get( ItemPool.LOUDMOUTH_LARRY, 1 );
	public static final AdventureResult PLASMA_BALL = ItemPool.get( ItemPool.PLASMA_BALL, 1 );

	public static final int [] campgroundItems =
	{
		// Housing
		ItemPool.BIG_ROCK,
		ItemPool.NEWBIESPORT_TENT,
		ItemPool.BARSKIN_TENT,
		ItemPool.COTTAGE,
		ItemPool.BRICKO_PYRAMID,
		ItemPool.HOUSE,
		ItemPool.SANDCASTLE,
		ItemPool.GIANT_FARADAY_CAGE,
		ItemPool.TWIG_HOUSE,
		ItemPool.GINGERBREAD_HOUSE,
		ItemPool.HOBO_FORTRESS,
		ItemPool.SNOW_FORT,
		ItemPool.ELEVENT,
		ItemPool.RESIDENCE_CUBE,

		// Bedding
		ItemPool.BEANBAG_CHAIR,
		ItemPool.COLD_BEDDING,
		ItemPool.GAUZE_HAMMOCK,
		ItemPool.HOT_BEDDING,
		ItemPool.LAZYBONES_RECLINER,
		ItemPool.SLEAZE_BEDDING,
		ItemPool.SPOOKY_BEDDING,
		ItemPool.STENCH_BEDDING,
		ItemPool.SLEEPING_STOCKING,
		ItemPool.SALTWATERBED,
		ItemPool.SPIRIT_BED,

		// Inside dwelling: maids
		ItemPool.MAID,
		ItemPool.CLOCKWORK_MAID,

		// Inside dwelling: miscellaneous
		// (Certificate of Participation)
		// (Shiny Certificate of Participation)
		ItemPool.BONSAI_TREE,
		ItemPool.CUCKOO_CLOCK,
		ItemPool.FENG_SHUI,
		ItemPool.LED_CLOCK,
		ItemPool.LUCKY_CAT_STATUE,
		ItemPool.MEAT_GLOBE,
		ItemPool.PICTURE_OF_YOU,
		ItemPool.TIN_ROOF,

		// Inside dwelling: "Tasteful" items
		ItemPool.BLACK_BLUE_LIGHT,
		ItemPool.LOUDMOUTH_LARRY,
		ItemPool.PLASMA_BALL,

		// Kitchen
		ItemPool.SHAKER,
		ItemPool.COCKTAIL_KIT,
		ItemPool.BARTENDER,
		ItemPool.CLOCKWORK_BARTENDER,
		ItemPool.OVEN,
		ItemPool.RANGE,
		ItemPool.CHEF,
		ItemPool.CLOCKWORK_CHEF,

		// Workshed
		ItemPool.CHEMISTRY_LAB,
		ItemPool.INDUCTION_OVEN,
		ItemPool.LP_ROM_BURNER,
		ItemPool.HIGH_EFFICIENCY_STILL,
		ItemPool.AUTO_ANVIL,
		ItemPool.JACKHAMMER_DRILL_PRESS,
		ItemPool.SNOW_MACHINE,
		ItemPool.SPINNING_WHEEL,
		ItemPool.DNA_LAB,
		ItemPool.MAYO_CLINIC,

		// Garden
		ItemPool.PUMPKIN,
		ItemPool.HUGE_PUMPKIN,
		ItemPool.GINORMOUS_PUMPKIN,
		ItemPool.PEPPERMINT_SPROUT,
		ItemPool.GIANT_CANDY_CANE,

		// Outside dwelling
		ItemPool.MEAT_GOLEM,
		ItemPool.PAGODA_PLANS,
		ItemPool.SCARECROW,
		ItemPool.TOILET_PAPER,
		ItemPool.HAUNTED_DOGHOUSE,
		ItemPool.WITCHESS_SET,

		// Special item that aids resting
		ItemPool.COMFY_BLANKET,
	};

	public static final int [] transientFurnishings =
	{
		// Bedding
		ItemPool.BEANBAG_CHAIR,
		ItemPool.COLD_BEDDING,
		ItemPool.GAUZE_HAMMOCK,
		ItemPool.HOT_BEDDING,
		ItemPool.LAZYBONES_RECLINER,
		ItemPool.SLEAZE_BEDDING,
		ItemPool.SPOOKY_BEDDING,
		ItemPool.STENCH_BEDDING,
		ItemPool.SLEEPING_STOCKING,
		ItemPool.SALTWATERBED,
		ItemPool.SPIRIT_BED,

		// Inside dwelling: miscellaneous
		ItemPool.BONSAI_TREE,
		ItemPool.CUCKOO_CLOCK,
		ItemPool.FENG_SHUI,
		ItemPool.LED_CLOCK,
		ItemPool.LUCKY_CAT_STATUE,
		ItemPool.MEAT_GLOBE,
		ItemPool.TIN_ROOF,

		// Inside dwelling: "Tasteful" items
		ItemPool.BLACK_BLUE_LIGHT,
		ItemPool.LOUDMOUTH_LARRY,
		ItemPool.PLASMA_BALL,
	};

	public static final AdventureResult PUMPKIN = ItemPool.get( ItemPool.PUMPKIN, 1 );
	public static final AdventureResult HUGE_PUMPKIN = ItemPool.get( ItemPool.HUGE_PUMPKIN, 1 );
	public static final AdventureResult GINORMOUS_PUMPKIN = ItemPool.get( ItemPool.GINORMOUS_PUMPKIN, 1 );
	public static final AdventureResult PEPPERMINT_SPROUT = ItemPool.get( ItemPool.PEPPERMINT_SPROUT, 1 );
	public static final AdventureResult GIANT_CANDY_CANE = ItemPool.get( ItemPool.GIANT_CANDY_CANE, 1 );
	public static final AdventureResult SKELETON = ItemPool.get( ItemPool.SKELETON, 1 );
	public static final AdventureResult BARLEY = ItemPool.get( ItemPool.BARLEY, 1 );
	public static final AdventureResult BEER_LABEL = ItemPool.get( ItemPool.FANCY_BEER_LABEL, 1 );
	public static final AdventureResult ICE_HARVEST = ItemPool.get( ItemPool.ICE_HARVEST, 1 );
	public static final AdventureResult FROST_FLOWER = ItemPool.get( ItemPool.FROST_FLOWER, 1 );

	private enum CropType
	{
		PUMPKIN,
		PEPPERMINT,
		SKELETON,
		BEER,
		WINTER,
		;

		@Override
		public String toString()
		{
			return this.name().toLowerCase();
		}
	}

	private static final HashMap<AdventureResult, CropType> CROPMAP = new HashMap<AdventureResult, CropType>();

	static
	{
		CROPMAP.put( PUMPKIN, CropType.PUMPKIN );
		CROPMAP.put( HUGE_PUMPKIN, CropType.PUMPKIN );
		CROPMAP.put( GINORMOUS_PUMPKIN, CropType.PUMPKIN );
		CROPMAP.put( PEPPERMINT_SPROUT, CropType.PEPPERMINT );
		CROPMAP.put( GIANT_CANDY_CANE, CropType.PEPPERMINT );
		CROPMAP.put( SKELETON, CropType.SKELETON );
		CROPMAP.put( BARLEY, CropType.BEER );
		CROPMAP.put( BEER_LABEL, CropType.BEER );
		CROPMAP.put( ICE_HARVEST, CropType.WINTER );
		CROPMAP.put( FROST_FLOWER, CropType.WINTER );
	}

	public static final List<Integer> workshedItems = new ArrayList<Integer>();

	static
	{
		CampgroundRequest.workshedItems.add( ItemPool.JACKHAMMER_DRILL_PRESS );
		CampgroundRequest.workshedItems.add( ItemPool.AUTO_ANVIL );
		CampgroundRequest.workshedItems.add( ItemPool.INDUCTION_OVEN );
		CampgroundRequest.workshedItems.add( ItemPool.CHEMISTRY_LAB );
		CampgroundRequest.workshedItems.add( ItemPool.HIGH_EFFICIENCY_STILL );
		CampgroundRequest.workshedItems.add( ItemPool.LP_ROM_BURNER );
		CampgroundRequest.workshedItems.add( ItemPool.SNOW_MACHINE );
		CampgroundRequest.workshedItems.add( ItemPool.SPINNING_WHEEL );
		CampgroundRequest.workshedItems.add( ItemPool.DNA_LAB );
		CampgroundRequest.workshedItems.add( ItemPool.MAYO_CLINIC );
	}

	public static final AdventureResult [] CROPS =
	{
		CampgroundRequest.PUMPKIN,
		CampgroundRequest.HUGE_PUMPKIN,
		CampgroundRequest.GINORMOUS_PUMPKIN,
		CampgroundRequest.PEPPERMINT_SPROUT,
		CampgroundRequest.GIANT_CANDY_CANE,
		CampgroundRequest.SKELETON,
		CampgroundRequest.BARLEY,
		CampgroundRequest.BEER_LABEL,
		CampgroundRequest.ICE_HARVEST,
		CampgroundRequest.FROST_FLOWER,
	};

	public static void reset()
	{
		KoLConstants.campground.clear();
		CampgroundRequest.currentDwellingLevel = 0;
		CampgroundRequest.currentDwelling = null;
		CampgroundRequest.currentBed = null;
		CampgroundRequest.currentWorkshedItem = null;
	}

	private final String action;

	/**
	 * Constructs a new <code>CampgroundRequest</code> with the specified
	 * action in mind.
	 */

	// campground.php?action=garden&pwd

	public CampgroundRequest( final String action )
	{
		super( "campground.php" );
		this.addFormField( "action", action );
		this.action = action;
	}

	/**
	 * Constructs a new <code>CampgroundRequest</code>.
	 */

	public CampgroundRequest()
	{
		// Not available in Ed and some Limitmodes
		this( "inspectdwelling" );
	}

	@Override
	public int getAdventuresUsed()
	{
		return this.action.equals( "rest" ) &&
			Preferences.getInteger( "timesRested" ) >= KoLCharacter.freeRestsAvailable() ? 1 : 0;
	}

	public static void setCampgroundItem( final int itemId, int count )
	{
		CampgroundRequest.setCampgroundItem( ItemPool.get( itemId, count ) );
	}

	private static void setCampgroundItem( final AdventureResult item )
	{
		int i = KoLConstants.campground.indexOf( item );
		if ( i != -1 )
		{
			AdventureResult old = (AdventureResult)KoLConstants.campground.get( i );
			if ( old.getCount() == item.getCount() )
			{
				return;
			}
			KoLConstants.campground.remove( i );
		}
		KoLConstants.campground.add( item );
	}

	public static void removeCampgroundItem( AdventureResult item )
	{
		int i = KoLConstants.campground.indexOf( item );
		if ( i != -1 )
		{
			KoLConstants.campground.remove( i );
		}
	}

	private static int getCropIndex()
	{
		for ( int i = 0; i < CROPS.length; ++i )
		{
			int index = KoLConstants.campground.indexOf( CROPS[ i ] );
			if ( index != -1 )
			{
				return index;
			}
		}
		return -1;
	}

	public static AdventureResult getCrop()
	{
		int i = CampgroundRequest.getCropIndex();
		return i != -1 ? (AdventureResult)KoLConstants.campground.get( i ) : null;
	}

	public static boolean hasCropOrBetter( final String crop )
	{
		// Get current crop, if any
		AdventureResult current = CampgroundRequest.getCrop();
		if ( current == null || current.getCount() == 0 )
		{
			// Nothing in your garden or no garden.
			return false;
		}

		// We want whatever is there.  Since we made it this far,
		// we have something to pick.
		if ( crop.equals( "any" ) )
		{
			return true;
		}

		// If it equals the desired crop, peachy. Or is it pumpkiny?
		String currentName = current.getName();
		if ( crop.equals( currentName )  )
		{
			return true;
		}

		// Iterate through CROPS.
		for ( int i = 0; i < CROPS.length; ++i )
		{
			String cropName = CROPS[ i ].getName();
			// We found the current crop before we found the
			// desired crop. Not good enough.
			if ( cropName.equals( currentName ) )
			{
				return false;
			}
			// We found the desired crop before we found the
			// current crop - which is therefore better IFF its type is the same.
			if ( cropName.equals( crop ) )
			{
				return CROPMAP.get( CROPS[ i ] ) == CROPMAP.get( current );
			}
		}

		// Shouldn't get here - didn't find either the current or the desired crop
		return false;
	}

	public static void clearCrop()
	{
		int i = CampgroundRequest.getCropIndex();
		if ( i != -1 )
		{
			KoLConstants.campground.remove( i );
		}
	}

	public static void harvestCrop()
	{
		AdventureResult crop = CampgroundRequest.getCrop();
		if ( crop != null && crop.getCount() > 0 )
		{
			RequestThread.postRequest( new CampgroundRequest( "garden" ) );
		}
	}

	public static void useSpinningWheel()
	{
		if ( CampgroundRequest.currentWorkshedItem != null &&
		     CampgroundRequest.currentWorkshedItem.getItemId() == ItemPool.SPINNING_WHEEL )
		{
			RequestThread.postRequest( new CampgroundRequest( "spinningwheel" ) );
		}
	}

	@Override
	public void run()
	{
		if ( this.action.equals( "rest" ) &&
		     KoLCharacter.getCurrentHP() == KoLCharacter.getMaximumHP() &&
		     KoLCharacter.getCurrentMP() == KoLCharacter.getMaximumMP() &&
		     !KoLConstants.activeEffects.contains( KoLAdventure.BEATEN_UP ) )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "You don't need to rest." );
			return;
		}

		if ( this.getAdventuresUsed() > KoLCharacter.getAdventuresLeft() )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "You don't have any time left for that" );
			return;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		CampgroundRequest.parseResponse( this.getURLString(), this.responseText );
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		// Workshed may be redirected to Shop if Mayo Clinic installed
		return action != null && action.equals( "workshed" );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		// Workshed may redirect to shop.php
		if ( urlString.startsWith( "shop.php" ) )
		{
			NPCPurchaseRequest.parseShopResponse( urlString, responseText );
			return;
		}

		if ( !urlString.startsWith( "campground.php" ) )
		{
			return;
		}

		if ( urlString.contains( "smashstone=Yep" ) )
		{
			// You shatter the <b>Magical Mystical Hippy Stone</b>.
			if ( responseText.contains( "You shatter the <b>Magical Mystical Hippy Stone</b>" ) )
			{
				KoLCharacter.setHippyStoneBroken( true );
				KoLCharacter.setAttacksLeft( 10 );
			}
			return;
		}

		Matcher matcher= GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			CampgroundRequest.parseCampground( responseText );
			return;
		}

		String action = matcher.group(1);

		// A request can have both action=bookshelf and preaction=yyy.
		// Check for that.
		if ( action.equals( "bookshelf" ) && matcher.find() )
		{
			action = matcher.group(1);
		}

		if ( action.equals( "bookshelf" ) )
		{
			// No preaction. Look at books.
			CampgroundRequest.parseBookTitles( responseText );
			return;
		}

		if ( action.endsWith( "powerelvibratoportal" ) )
		{
			PortalRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( action.startsWith( "telescope" ) )
		{
			TelescopeRequest.parseResponse( urlString, responseText );
			return;
		}

		// Using a book skill from the Mystic Bookshelf does this:
		//   campground.php?quantity=1&preaction=summonlovesongs&pwd
		//
		// Using a book skill from the skill menu redirects to the
		// above URL with an additional field:
		//   skilluse=1

		// Combining clip arts does this:
		//   campground.php?action=bookshelf&preaction=combinecliparts&clip1=05&clip2=05&clip3=03&pwd

		if ( action.startsWith( "summon" ) ||
		     action.equals( "combinecliparts" ) )
		{
			UseSkillRequest.parseResponse( urlString, responseText );
			return;
		}

		if ( action.equals( "rest" ) )
		{
			Preferences.increment( "timesRested", 1 );

			// Your black-and-blue light cycles wildly between
			// black and blue, then emits a shower of sparks as it
			// goes permanently black.
			if ( responseText.contains( "goes permanently black" ) )
			{
				CampgroundRequest.removeCampgroundItem( BLACK_BLUE_LIGHT );
			}

			// Your blue plasma ball crackles weakly, emits a whine
			// that sounds like "pika...pika...pika..." and goes
			// dark.
			if ( responseText.contains( "crackles weakly" ) )
			{
				CampgroundRequest.removeCampgroundItem( PLASMA_BALL );
			}

			// Your Loudmouth Larry Lamprey twitches and flops
			// wildly, singing "Daisy, Daisy, tell me your answer
			// true," in ever-slower, distorted loops. Looks like
			// it's ready to go to its eternal fishy reward.
			if ( responseText.contains( "eternal fishy reward" ) )
			{
				CampgroundRequest.removeCampgroundItem( LOUDMOUTH_LARRY );
			}

			// You dream that your teeth fall out, and you put them
			// in your pocket for safe keeping. Fortunately, when
			// you wake up, you appear to have grown a new set.
			if ( responseText.contains( "your teeth fall out" ) )
			{
				ResultProcessor.processItem( ItemPool.LOOSE_TEETH, 1 );
			}

			// "Hey," he says, "youse got some teeth. T'anks. Here
			// youse goes."
			if ( responseText.contains( "youse got some teeth" ) )
			{
				ResultProcessor.processItem( ItemPool.LOOSE_TEETH, -1 );
			}

			Matcher m = HOUSING_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				KoLCharacter.updateFreeRests( m.group(3) != null );
			}

			return;
		}

		if ( action.equals( "garden" ) )
		{
			CampgroundRequest.clearCrop();
			CampgroundRequest.parseCampground( responseText );
			return;
		}

		if ( action.equals( "inspectdwelling" ) )
		{
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseDwelling( responseText );
			return;
		}

		if ( action.equals( "inspectkitchen" ) )
		{
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseKitchen( responseText );
			return;
		}

		if ( action.equals( "workshed" ) )
		{
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseWorkshed( responseText );
			return;
		}

		if ( action.equals( "dnapotion" ) )
		{
			if ( responseText.contains( "little bottle of gene tonic" ) )
			{
				Preferences.increment( "_dnaPotionsMade", 1 );
			}
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseWorkshed( responseText );
			return;
		}

		if ( action.equals( "dnainject" ) )
		{
			if ( responseText.contains( "abominable genetic hybrid" ) )
			{
				Preferences.setBoolean( "_dnaHybrid", true );
				Preferences.setString( "_dnaSyringe", "" );
			}
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseWorkshed( responseText );
			return;
		}

		if ( action.equals( "spinningwheel" ) )
		{
			// You work the spinning wheel and spin some air into Meat.
			// You can't spin any more air into Meat today.
			if ( responseText.contains( "air into Meat" ) )
			{
				Preferences.setBoolean( "_spinningWheel", true );
			}
			CampgroundRequest.parseCampground( responseText );
			CampgroundRequest.parseWorkshed( responseText );
			return;
		}
	}

	private static final void parseCampground( final String responseText )
	{
		KoLCharacter.setTelescope( findImage( responseText, "telescope.gif", ItemPool.TELESCOPE ) );
		KoLCharacter.setBookshelf( responseText.contains( "action=bookshelf" ) );
		KoLCharacter.setHippyStoneBroken( responseText.contains( "smashstone.gif" ) );

		findImage( responseText, "pagoda.gif", ItemPool.PAGODA_PLANS );
		findImage( responseText, "scarecrow.gif", ItemPool.SCARECROW );
		findImage( responseText, "golem.gif", ItemPool.MEAT_GOLEM );
		findImage( responseText, "doghouse.gif", ItemPool.HAUNTED_DOGHOUSE );
		findImage( responseText, "chesstable.gif", ItemPool.WITCHESS_SET );

		findImage( responseText, "teatree", ItemPool.POTTED_TEA_TREE );
		if ( responseText.contains( "teatree_used.gif" ) )
		{
			Preferences.setBoolean( "_pottedTeaTreeUsed", true );
		}

		boolean maidFound = false;
		if ( !maidFound ) maidFound = findImage( responseText, "maid.gif", ItemPool.MAID );
		if ( !maidFound ) maidFound = findImage( responseText, "maid2.gif", ItemPool.CLOCKWORK_MAID );

		boolean gardenFound = false;
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_0.gif", ItemPool.PUMPKIN, 0 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_1.gif", ItemPool.PUMPKIN, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_2.gif", ItemPool.PUMPKIN, 2 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_3.gif", ItemPool.PUMPKIN, 3 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_4.gif", ItemPool.PUMPKIN, 4 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_giant.gif", ItemPool.HUGE_PUMPKIN, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pumpkinpatch_ginormous.gif", ItemPool.GINORMOUS_PUMPKIN, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pepperpatch_0.gif", ItemPool.PEPPERMINT_SPROUT, 0 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pepperpatch_1.gif", ItemPool.PEPPERMINT_SPROUT, 3 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pepperpatch_2.gif", ItemPool.PEPPERMINT_SPROUT, 6 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pepperpatch_3.gif", ItemPool.PEPPERMINT_SPROUT, 9 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pepperpatch_4.gif", ItemPool.PEPPERMINT_SPROUT, 12 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "pepperpatch_huge.gif", ItemPool.GIANT_CANDY_CANE, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden0.gif", ItemPool.SKELETON, 0 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden1.gif", ItemPool.SKELETON, 5 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden2.gif", ItemPool.SKELETON, 10 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden3.gif", ItemPool.SKELETON, 15 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden4.gif", ItemPool.SKELETON, 20 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden5.gif", ItemPool.SKELETON, 25 );
		// This is day 6 for A Bone Garden.  It triggers a combat, so it should never be automatically picked.
		// Setting a negative number of items will make it possible to tell that it isn't empty.
		if ( !gardenFound ) gardenFound = findImage( responseText, "bonegarden_spoilzlul.gif", ItemPool.SKELETON, -1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden0.gif", ItemPool.BARLEY, 0 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden1.gif", ItemPool.BARLEY, 3 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden2.gif", ItemPool.FANCY_BEER_LABEL, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden3.gif", ItemPool.FANCY_BEER_LABEL, 2 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden4.gif", ItemPool.FANCY_BEER_LABEL, 3 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden5.gif", ItemPool.FANCY_BEER_LABEL, 4 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden6.gif", ItemPool.FANCY_BEER_LABEL, 5 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "beergarden7.gif", ItemPool.FANCY_BEER_LABEL, 6 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden0.gif", ItemPool.ICE_HARVEST, 0 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden1.gif", ItemPool.ICE_HARVEST, 3 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden2.gif", ItemPool.ICE_HARVEST, 6 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden3.gif", ItemPool.FROST_FLOWER, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden4.gif", ItemPool.FROST_FLOWER, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden5.gif", ItemPool.FROST_FLOWER, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden6.gif", ItemPool.FROST_FLOWER, 1 );
		if ( !gardenFound ) gardenFound = findImage( responseText, "wintergarden7.gif", ItemPool.FROST_FLOWER, 1 );

		Matcher jungMatcher = JUNG_PATTERN.matcher( responseText );
		if ( jungMatcher.find() )
		{
			int jungLink = StringUtilities.parseInt( jungMatcher.group( 1 ) );
			switch ( jungLink )
			{
			case 1:
				CampgroundRequest.setCampgroundItem( ItemPool.SUSPICIOUS_JAR, 1 );
				break;
			case 2:
				CampgroundRequest.setCampgroundItem( ItemPool.GOURD_JAR , 1 );
				break;
			case 3:
				CampgroundRequest.setCampgroundItem( ItemPool.MYSTIC_JAR , 1 );
				break;
			case 4:
				CampgroundRequest.setCampgroundItem( ItemPool.OLD_MAN_JAR , 1 );
				break;
			case 5:
				CampgroundRequest.setCampgroundItem( ItemPool.ARTIST_JAR , 1 );
				break;
			case 6:
				CampgroundRequest.setCampgroundItem( ItemPool.MEATSMITH_JAR , 1 );
				break;
			case 7:
				CampgroundRequest.setCampgroundItem( ItemPool.JICK_JAR , 1 );
				break;
			}
		}
		else
		{
			Preferences.setBoolean( "_psychoJarUsed", false );
		}

	}

	private static final void parseDwelling( final String responseText )
	{
		Matcher m = HOUSING_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to parse housing!" );
			return;
		}

		String dwelling = m.group( 1 );
		if ( dwelling.equals( "a" ) )
		{
			dwelling = "10";
		}
		else if ( dwelling.equals( "b" ) )
		{
			dwelling = "11";
		}
		else if ( dwelling.equals( "c" ) )
		{
			dwelling = "12";
		}
		else if ( dwelling.equals( "d" ) )
		{
			dwelling = "13";
		}
		else if ( dwelling.equals( "e" ) )
		{
			dwelling = "14";
		}

		int itemId = -1;
		switch ( StringUtilities.parseInt( dwelling ) )
		{
		case 0:
			// placeholder for "the ground"
			CampgroundRequest.currentDwelling = BIG_ROCK;
			CampgroundRequest.currentDwellingLevel = 0;
			break;
		case 1:
			itemId = ItemPool.NEWBIESPORT_TENT;
			break;
		case 2:
			itemId = ItemPool.BARSKIN_TENT;
			break;
		case 3:
			itemId = ItemPool.COTTAGE;
			break;
		case 4:
			itemId = ItemPool.HOUSE;
			break;
		case 5:
			itemId = ItemPool.SANDCASTLE;
			break;
		case 6:
			itemId = ItemPool.TWIG_HOUSE;
			break;
		case 7:
			itemId = ItemPool.HOBO_FORTRESS;
			break;
		case 8:
			itemId = ItemPool.GINGERBREAD_HOUSE;
			break;
		case 9:
			itemId = ItemPool.BRICKO_PYRAMID;
			break;
		case 10:
			itemId = ItemPool.GINORMOUS_PUMPKIN;
			break;
		case 11:
			itemId = ItemPool.GIANT_FARADAY_CAGE;
			break;
		case 12:
			itemId = ItemPool.SNOW_FORT;
			break;
		case 13:
			itemId = ItemPool.ELEVENT;
			break;
		case 14:
			itemId = ItemPool.RESIDENCE_CUBE;
			break;
		default:
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unrecognized housing type (" + CampgroundRequest.currentDwellingLevel + ")!" );
			break;
		}

		if ( itemId != -1 )
		{
			CampgroundRequest.setCurrentDwelling( itemId );
		}

		if ( m.group( 2 ) != null )
		{
			CampgroundRequest.setCampgroundItem( ItemPool.TOILET_PAPER, 1 );
		}

		KoLCharacter.updateFreeRests( m.group(3) != null );

		int startIndex = responseText.indexOf( "Your dwelling has the following stuff" );
		int endIndex = responseText.indexOf( "<b>Your Campsite</b>", startIndex + 1 );
		if ( startIndex > 0 && endIndex > 0 )
		{
			m = FURNISHING_PATTERN.matcher( responseText.substring( startIndex, endIndex ) );
			while ( m.find() )
			{
				String name = m.group(1);

				if ( name.equals( "Really Good Feng Shui" ) )
				{
					name = "Feng Shui for Big Dumb Idiots";
				}

				AdventureResult ar = ItemPool.get( name, 1 );
				if ( CampgroundRequest.isBedding( ar.getItemId() ) )
				{
					CampgroundRequest.setCurrentBed( ar );
				}

				CampgroundRequest.setCampgroundItem( ar );
			}
		}
	}

	private static final void parseKitchen( final String responseText )
	{
		boolean hasOven = findImage( responseText, "ezcook.gif", ItemPool.OVEN );
		KoLCharacter.setOven( hasOven );

		boolean hasRange = findImage( responseText, "oven.gif", ItemPool.RANGE );
		KoLCharacter.setRange( hasRange );

		boolean hasChef =
			findImage( responseText, "chefinbox.gif", ItemPool.CHEF ) ||
			findImage( responseText, "cchefbox.gif", ItemPool.CLOCKWORK_CHEF );
		KoLCharacter.setChef( hasChef );

		boolean hasShaker = findImage( responseText, "shaker.gif", ItemPool.SHAKER );
		KoLCharacter.setShaker( hasShaker );

		boolean hasCocktailKit = findImage( responseText, "cocktailkit.gif", ItemPool.COCKTAIL_KIT );
		KoLCharacter.setCocktailKit( hasCocktailKit );

		boolean hasBartender =
			findImage( responseText, "bartinbox.gif", ItemPool.BARTENDER ) ||
			findImage( responseText, "cbartbox.gif", ItemPool.CLOCKWORK_BARTENDER );
		KoLCharacter.setBartender( hasBartender );

		boolean hasSushiMat = findImage( responseText, "sushimat.gif", ItemPool.SUSHI_ROLLING_MAT );
		KoLCharacter.setSushiMat( hasSushiMat );
	}

	private static final void parseWorkshed( final String responseText )
	{
		// Do we need to remember these things in KoLCharacter?
		if ( findImage( responseText, "wbchemset.gif", ItemPool.CHEMISTRY_LAB ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.CHEMISTRY_LAB );
		}
		else if ( findImage( responseText, "wboven.gif", ItemPool.INDUCTION_OVEN ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.INDUCTION_OVEN );
		}
		else if ( findImage( responseText, "wblprom.gif", ItemPool.LP_ROM_BURNER ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.LP_ROM_BURNER );
		}
		else if ( findImage( responseText, "wbstill.gif", ItemPool.HIGH_EFFICIENCY_STILL ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.HIGH_EFFICIENCY_STILL );
		}
		else if ( findImage( responseText, "wbanvil.gif", ItemPool.AUTO_ANVIL ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.AUTO_ANVIL );
		}
		else if ( findImage( responseText, "wbdrillpress.gif", ItemPool.JACKHAMMER_DRILL_PRESS ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.JACKHAMMER_DRILL_PRESS );
		}
		else if ( findImage( responseText, "snowmachine.gif", ItemPool.SNOW_MACHINE ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.SNOW_MACHINE );
		}
		else if ( findImage( responseText, "spinningwheel.gif", ItemPool.SPINNING_WHEEL ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.SPINNING_WHEEL );
		}
		else if ( findImage( responseText, "genelab.gif", ItemPool.DNA_LAB ) )
		{
			CampgroundRequest.setCurrentWorkshedItem( ItemPool.DNA_LAB );
			Matcher dnaMatcher = DNA_PATTERN.matcher( responseText );
			if ( dnaMatcher.find() )
			{
				Preferences.setString( "dnaSyringe", dnaMatcher.group( 1 ) );
			}
			else if ( responseText.contains( "DNA extraction syringe is currently empty" ) )
			{
				Preferences.setString( "dnaSyringe", "" );
			}
			if ( responseText.contains( "lab needs to reorder the supplies" ) )
			{
				Preferences.setInteger( "_dnaPotionsMade", 3 );
			}
			if ( responseText.contains( "horrible abomination once today" ) )
			{
				Preferences.setBoolean( "_dnaHybrid", true );
			}
		}
		// Mayo Clinic redirects to Store, so handle in NPCPurchaseRequest
	}

	private static boolean findImage( final String responseText, final String filename, final int itemId )
	{
		return CampgroundRequest.findImage( responseText, filename, itemId, false );
	}

	private static boolean findImage( final String responseText, final String filename, final int itemId, boolean allowMultiple )
	{
		int count = 0;
		int i = responseText.indexOf( filename );
		while ( i != -1 )
		{
			++count;
			i = responseText.indexOf( filename, i + 1 );
		}

		if ( count > 0 )
		{
			CampgroundRequest.setCampgroundItem( itemId, allowMultiple ? count : 1 );
		}

		return ( count > 0 );
	}

	private static boolean findImage( final String responseText, final String filename, final int itemId, int count )
	{
		if ( !responseText.contains( filename ) )
		{
			return false;
		}

		CampgroundRequest.setCampgroundItem( itemId, count );

		return true;
	}

	public static AdventureResult getCurrentDwelling()
	{
		return currentDwelling == null ? BIG_ROCK : currentDwelling;
	}

	public static int getCurrentDwellingLevel()
	{
		return currentDwellingLevel;
	}

	public static void setCurrentDwelling( int itemId )
	{
		CampgroundRequest.currentDwelling = ItemPool.get( itemId, 1 );
		CampgroundRequest.currentDwellingLevel = CampgroundRequest.dwellingLevel( itemId );
	}

	public static void destroyFurnishings()
	{
		CampgroundRequest.setCurrentBed( null );
		for ( int itemId : CampgroundRequest.transientFurnishings )
		{
			CampgroundRequest.removeCampgroundItem( ItemPool.get( itemId, 1 ) );
		}
	}

	public static AdventureResult getCurrentBed()
	{
		return CampgroundRequest.currentBed;
	}

	public static void setCurrentBed( AdventureResult bed )
	{
		if ( CampgroundRequest.currentBed != null )
		{
			CampgroundRequest.removeCampgroundItem( CampgroundRequest.currentBed );
		}
		CampgroundRequest.currentBed = bed;
	}

	public static AdventureResult getCurrentWorkshedItem()
	{
		return currentWorkshedItem;
	}

	public static void setCurrentWorkshedItem( int itemId )
	{
		AdventureResult workshedItem = ItemPool.get( itemId, 1 );
		CampgroundRequest.setCurrentWorkshedItem( workshedItem );
	}

	public static void setCurrentWorkshedItem( AdventureResult workshedItem )
	{
		if ( workshedItem.equals( CampgroundRequest.getCurrentWorkshedItem() ) )
		{
			return;
		}

		if ( CampgroundRequest.getCurrentWorkshedItem() != null )
		{
			CampgroundRequest.removeCampgroundItem( CampgroundRequest.getCurrentWorkshedItem() );
		}
		CampgroundRequest.currentWorkshedItem = workshedItem;
		CampgroundRequest.setCampgroundItem( workshedItem );
		if ( workshedItem.getItemId() == ItemPool.MAYO_CLINIC )
		{
			ConcoctionDatabase.refreshConcoctions();
		}
	}

	public static boolean isDwelling( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.NEWBIESPORT_TENT:
		case ItemPool.BARSKIN_TENT:
		case ItemPool.COTTAGE:
		case ItemPool.BRICKO_PYRAMID:
		case ItemPool.HOUSE:
		case ItemPool.SANDCASTLE:
		case ItemPool.TWIG_HOUSE:
		case ItemPool.GINGERBREAD_HOUSE:
		case ItemPool.HOBO_FORTRESS:
		case ItemPool.GINORMOUS_PUMPKIN:
		case ItemPool.GIANT_FARADAY_CAGE:
		case ItemPool.SNOW_FORT:
		case ItemPool.ELEVENT:
		case ItemPool.RESIDENCE_CUBE:
			return true;
		}
		return false;
	}

	public static int dwellingLevel( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.NEWBIESPORT_TENT:
			return 1;
		case ItemPool.BARSKIN_TENT:
			return 2;
		case ItemPool.COTTAGE:
			return 3;
		case ItemPool.BRICKO_PYRAMID:
			return 4;
		case ItemPool.HOUSE:
			return 5;
		case ItemPool.SANDCASTLE:
			return 6;
		case ItemPool.GINORMOUS_PUMPKIN:
			return 7;
		case ItemPool.TWIG_HOUSE:
			return 8;
		case ItemPool.GINGERBREAD_HOUSE:
			return 9;
		case ItemPool.HOBO_FORTRESS:
			return 10;
		case ItemPool.GIANT_FARADAY_CAGE:
			return 11;
		case ItemPool.SNOW_FORT:
			return 12;
		case ItemPool.ELEVENT:
			return 13;
		case ItemPool.RESIDENCE_CUBE:
			return 14;
		}
		return 0;
	}

	public static boolean isBedding( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.BEANBAG_CHAIR:
		case ItemPool.GAUZE_HAMMOCK:
		case ItemPool.LAZYBONES_RECLINER:
		case ItemPool.SLEEPING_STOCKING:
		case ItemPool.HOT_BEDDING:
		case ItemPool.COLD_BEDDING:
		case ItemPool.STENCH_BEDDING:
		case ItemPool.SPOOKY_BEDDING:
		case ItemPool.SLEAZE_BEDDING:
		case ItemPool.SALTWATERBED:
		case ItemPool.SPIRIT_BED:
			return true;
		}
		return false;
	}

	public static boolean isWorkshedItem( final int itemId )
	{
		return CampgroundRequest.workshedItems.contains( itemId );
	}

	private static final String[][] BOOKS =
	{
		{
			"Tome of Snowcone Summoning",
			"Summon Snowcones"
		},
		{
			"Tome of Sticker Summoning",
			"Summon Stickers"
		},
		{
			"Tome of Sugar Shummoning",
			"Summon Sugar Sheets"
		},
		{
			"Tome of Clip Art",
			"Summon Clip Art"
		},
		{
			"Tome of Rad Libs",
			"Summon Rad Libs"
		},
		{
			"The Smith's Tome",
			"Summon Smithsness"
		},
		{
			// The bookshelf currently says:
			// "McPhee's Grimoire of Hilarious Item Summoning"
			// gives access to "Summon Hilarious Items".
			//
			// The item is currently named:
			// "McPhee's Grimoire of Hilarious Object Summoning"
			// and gives access to "Summon Hilarious Objects".
			"McPhee's Grimoire",
			"Summon Hilarious Objects",
		},
		{
			"Sp'n-Zor's Grimoire",
			"Summon Tasteful Items",
		},
		{
			"Sorcerers of the Shore Grimoire",
			"Summon Alice's Army Cards",
		},
		{
			"Thinknerd Grimoire",
			"Summon Geeky Gifts",
		},
		{
			"Libram of Candy Heart Summoning",
			"Summon Candy Heart",
		},
		{
			"Libram of Divine Favors",
			"Summon Party Favor"
		},
		{
			"Libram of Love Songs",
			"Summon Love Song"
		},
		{
			"Libram of BRICKOs",
			"Summon BRICKOs"
		},
		{
			"Gygaxian Libram",
			"Summon Dice"
		},
		{
			"Libram of Resolutions",
			"Summon Resolutions"
		},
		{
			"Libram of Pulled Taffy",
			"Summon Taffy"
		},
		{
			"Confiscator's Grimoire",
			"Summon Confiscated Things"
		},
	};

	private static void parseBookTitles( final String responseText )
	{
		if ( ( KoLCharacter.inBadMoon() ||
		       KoLCharacter.inAxecore() ||
		       KoLCharacter.inZombiecore() ||
		       KoLCharacter.isSneakyPete() ) &&
		     !KoLCharacter.kingLiberated() )
		{
			// You can't use Mr. Skills in Bad Moon
			// You can't use Mr. Skills as an Avatar of Boris or Sneaky Pete
			// You can't use Mr. Skills as a Zombie Master
			return;
		}

		String libram = null;
		for ( int i = 0; i < BOOKS.length; ++i )
		{
			String book = BOOKS[i][0];
			if ( responseText.contains( book ) )
			{
				String skill = BOOKS[i][1];
				KoLCharacter.addAvailableSkill( skill, true );
				if ( book.contains( "Libram" ) )
				{
					libram = skill;
				}
			}
		}

		if ( libram != null )
		{
			Matcher matcher = CampgroundRequest.LIBRAM_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				int cost = StringUtilities.parseInt( matcher.group(2) );
				SkillDatabase.setLibramSkillCasts( cost );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "campground.php" ) )
		{
			return false;
		}

		Matcher matcher= GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			// Simple visit. Nothing to log.
			return true;
		}

		String action = matcher.group(1);
		if ( action.equals( "bookshelf" ) )
		{
			// A request can have both action=bookshelf and preaction=yyy.
			// Check for that.
			if ( !matcher.find() )
			{
				// Nothing to log.
				return true;
			}
			action = matcher.group(1);
		}

		// Dispatch campground requests to other classes

		if ( action.endsWith( "elvibratoportal" ) )
		{
			return PortalRequest.registerRequest( urlString );
		}

		if ( action.startsWith( "telescope" ) )
		{
			return TelescopeRequest.registerRequest( urlString );
		}

		// campground.php?pwd&action=bookshelf&preaction=combinecliparts&clip1=05&clip2=05&clip3=03
		// 01 = DONUT
		// 02 = BOMB
		// 03 = KITTEN
		// 04 = WINE
		// 05 = CHEESE
		// 06 = LIGHT BULB
		// 07 = SNOWFLAKE
		// 08 = SKULL
		// 09 = CLOCK
		// 10 = HAMMER

		if ( action.startsWith( "summon" ) ||
		     action.equals( "combinecliparts" ) )
		{
			// Detect a redirection to campground.php from
			// skills.php. The first one was already logged.
			if ( urlString.contains( "skilluse=1" ) )
			{
				return true;
			}
			return UseSkillRequest.registerRequest( urlString );
		}

		// Dispatch campground requests from this class

		if ( action.equals( "inspectdwelling" ) ||
		     action.equals( "inspectkitchen" ) ||
		     action.equals( "workshed" ) )
		{
			// Nothing to log.
			return true;
		}

		String message = null;

		if ( action.equals( "garden" ) )
		{
			message = "Harvesting your garden";
		}
		else if ( action.equals( "spinningwheel" ) )
		{
			message = "Spinning Meat from air";
		}
		else if ( action.equals( "dnapotion" ) )
		{
			message = "Making a Gene Tonic";
		}
		else if ( action.equals( "dnainject" ) )
		{
			message = "Hybridizing yourself";
		}
		else if ( action.equals( "rest" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Rest in your dwelling";
		}
		else if ( action.equals( "witchess" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Your Witchess Set";
		}
		else
		{
			// Unknown action.
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );
		return true;
	}
}
