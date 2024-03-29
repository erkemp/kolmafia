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

package net.sourceforge.kolmafia.webui;

import java.util.ArrayList;
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.BanishManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ValhallaDecorator
{
	public static final void decorateGashJump( final String location, final StringBuffer buffer )
	{
		// ascend.php
		// ascend.php?alttext=communityservice

		if ( buffer.indexOf("<p>Are you") > -1 )
		{
			buffer.delete( buffer.indexOf( "<p>Are you" ), buffer.indexOf( "<p><center>" ) );
		}

		StringUtilities.singleStringReplace( buffer, "<p>Please", " Please" );

		StringBuffer predictions = new StringBuffer();

		predictions.append( "</center></td><td>&nbsp;&nbsp;&nbsp;&nbsp;</td>" );
		predictions.append( "<td><div style=\"padding-top: 10px; padding-left: 10px; padding-right: 10px; padding-bottom: 10px\"><font size=-1>" );
		HolidayDatabase.addPredictionHTML( predictions, new Date(), HolidayDatabase.getPhaseStep(), false );
		predictions.append( "</font></div></td></tr><tr><td colspan=3><br>" );
		predictions.append( KoLConstants.LINE_BREAK );
		predictions.append( KoLConstants.LINE_BREAK );

		StringUtilities.singleStringReplace( buffer, "</center><p>", predictions.toString() );

		// We remove the confirmation checkboxes and automatically submit those controls as "on"
		String oldButtons = "<input type=submit class=button value=\"Ascend\"> <input type=checkbox name=confirm> (confirm) <input type=checkbox name=confirm2> (seriously)";
		String newButtons = "<input type=submit class=button value=\"Ascend\"><input type=hidden name=confirm value=on><input type=hidden name=confirm2 value=on>";

		StringBuffer reminders = new StringBuffer();
		reminders.append( "<br><table>" );
		reminders.append( "<tr><td>" );
		reminders.append( newButtons );
		reminders.append( "</td></tr>" );
		reminders.append( "</table>" );
		reminders.append( "<br><table cellspacing=10 cellpadding=10><tr>" );

		ArrayList<String> skillList = new ArrayList<String>();
		ArrayList<UseSkillRequest> unpermedSkills = new ArrayList<UseSkillRequest>();
		for ( int i = 0; i < KoLConstants.availableSkills.size(); ++i )
		{
			UseSkillRequest skill = (UseSkillRequest) KoLConstants.availableSkills.get( i );
			skillList.add( String.valueOf( skill.getSkillId() ) );
			if ( !KoLConstants.permedSkills.contains( skill ) )
			{
				unpermedSkills.add( skill );
			}
		}

		reminders.append( "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Haven't Yet Permed</th></tr><tr><td align=center><font size=\"-1\">" );
		ValhallaDecorator.listPermableSkills( reminders, unpermedSkills );
		reminders.append( "</font></td></tr></table></td>" );

		StringBuffer buySkills = new StringBuffer();
		buySkills.append( "<td bgcolor=\"#eeffee\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Skills You Didn't Buy</th></tr><tr><td align=center><font size=\"-1\">" );
		int count = ValhallaDecorator.listPermanentSkills( buySkills, skillList, SkillDatabase.classSkillsBase() );
		buySkills.append( "</font></td></tr></table></td>" );
		// Don't show not purchasable skill list if nothing to show
		if ( count != 0 )
		{
			reminders.append( buySkills );
		}

		reminders.append( "<td bgcolor=\"#eeeeff\" valign=top><table><tr><th style=\"text-decoration: underline\" align=center>Common Stuff You Didn't Do</th></tr><tr><td align=center><font size=\"-1\">" );
		ValhallaDecorator.listCommonTasks( reminders );
		reminders.append( "</font></td></tr></table></td>" );

		reminders.append( "</tr></table><br><br>" );

		StringUtilities.singleStringReplace( buffer, oldButtons, reminders.toString() );

		return;
	}

	private static final void listPermableSkills( final StringBuffer buffer, final ArrayList unpermedSkills )
	{
		for ( int i = 0; i < unpermedSkills.size(); ++i )
		{
			UseSkillRequest skill = (UseSkillRequest)unpermedSkills.get( i );
			int skillId = skill.getSkillId();

			if ( !SkillDatabase.isPermable( skillId ) )
			{
				continue;
			}

			String skillName = skill.getSkillName();

			buffer.append( "<nobr>" );
			buffer.append( "<a onClick=\"skill('" );
			buffer.append( skillId );
			buffer.append( "');\">" );
			buffer.append( skillName );
			buffer.append( "</a>" );
			buffer.append( "</nobr><br>" );
		}
	}

	private static final int listPermanentSkills( final StringBuffer buffer, final ArrayList skillList, final int startingPoint )
	{
		int count = 0;
		for ( int i = 0; i < 100; ++i )
		{
			int skillId = startingPoint + i;

			String skillName = SkillDatabase.getSkillName( skillId );
			if ( skillName == null )
			{
				continue;
			}

			if ( !SkillDatabase.isPermable( skillId ) )
			{
				continue;
			}

			buffer.append( "<nobr>" );
			boolean alreadyPermed = skillList.contains( String.valueOf( skillId ) );
			if ( alreadyPermed )
			{
				buffer.append( "<font color=darkgray><s>" );
			}

			buffer.append( "<a onClick=\"skill('" );
			buffer.append( skillId );
			buffer.append( "');\">" );
			buffer.append( skillName );
			buffer.append( "</a>" );

			if ( alreadyPermed )
			{
				buffer.append( "</s></font>" );
			}

			buffer.append( "</nobr><br>" );
			count++;
		}
		return count;
	}

	private static final void listCommonTasks( final StringBuffer buffer )
	{
		RelayRequest.redirectedCommandURL = "/ascend.php";

		boolean hasGift = false;
		hasGift |= ValhallaDecorator.developerGift( buffer, ItemPool.RUBBER_EMO_ROE, "Veracity" );
		hasGift |= ValhallaDecorator.developerGift( buffer, ItemPool.RUBBER_WWTNSD_BRACELET, "Veracity" );
		hasGift |= ValhallaDecorator.developerGift( buffer, ItemPool.STUFFED_COCOABO, "holatuwol" );
		hasGift |= ValhallaDecorator.developerGift( buffer, ItemPool.D10, "bmaher" );
		if ( hasGift )
		{
			buffer.append( "<br />" );
		}

		int count = InventoryManager.getCount( ItemPool.INSTANT_KARMA );
		if ( count > 0 )
		{
			int banked = Preferences.getInteger( "bankedKarma" );
			buffer.append( "<nobr><a href=\"javascript:if(confirm('Are you sure you want to discard your Instant Karma?')) {singleUse('inventory.php?which=1&action=discard&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "&whichitem=");
			buffer.append( ItemPool.INSTANT_KARMA );
			buffer.append( "&ajax=1');discardKarma();}void(0);\">discard karma</a> (have <span id='haveKarma'>" );
			buffer.append( count );
			buffer.append( "</span>, banked <span id='bankedKarma'>" );
			buffer.append( banked );
			buffer.append( "</span>)</nobr><br>" );
		}

		if ( KoLCharacter.getZapper() != null )
		{
			buffer.append( "<nobr><a href=\"wand.php?whichwand=" );
			buffer.append( KoLCharacter.getZapper().getItemId() );
			buffer.append( "\">blow up your zap wand</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.DEAD_MIMIC ) )
		{
			buffer.append( "<nobr><a href=\"javascript:singleUse('inv_use.php?&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "&which=3&whichitem=" );
			buffer.append( ItemPool.DEAD_MIMIC );
			buffer.append( "&ajax=1')\">use your dead mimic</a></nobr><br>" );
		}

		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.BORIS_KEY, "Boris&#39;s" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.JARLSBERG_KEY, "Jarlsberg&#39;s" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.SNEAKY_PETE_KEY, "Sneaky Pete&#39;" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.DIGITAL_KEY, "digital" );
		ValhallaDecorator.checkForKeyLime( buffer, ItemPool.STAR_KEY, "star" );

		if ( InventoryManager.hasItem( ItemPool.BUBBLIN_STONE ) )
		{
			buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+1+aerated+diving+helmet&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">make an aerated diving helmet</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.CITADEL_SATCHEL ) )
		{
			buffer.append( "<nobr><a href=\"guild.php?place=paco\">complete white citadel quest by turning in White Citadel Satisfaction Satchel</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.THICK_PADDED_ENVELOPE ) )
		{
			buffer.append( "<nobr><a href=\"guild.php?place=paco\">complete dwarvish delivery quest by turning in thick padded envelope</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.DWARVISH_PUNCHCARD )   )
		{
			buffer.append( "<nobr><a href=\"dwarfcontraption.php\">acquire dwarvish war outfit piece</a></nobr><br>" );
		}

		if ( InventoryManager.hasItem( ItemPool.RAT_WHISKER )
			&& Preferences.getString( Quest.ARTIST.getPref() ).equals( QuestDatabase.FINISHED ) )
		{
			buffer.append( "<nobr><a href=\"place.php?whichplace=town_wrong&action=townwrong_artist_quest&subaction=whiskers\">" );
			buffer.append( "trade in rat whiskers for meat</a></nobr><br>" );
		}

		GenericRequest trophyCheck = new GenericRequest( "trophy.php" );
		trophyCheck.run();
		if ( !trophyCheck.responseText.contains( "You're not currently entitled to any trophies" ) )
		{
			buffer.append( "<nobr><a href=\"trophy.php\">buy trophies you're eligible for</a></nobr><br>" );
		}
		int ip = Preferences.getInteger( "lastGoofballBuy" );
		if ( KoLCharacter.getAscensions() > ip )
		{
			buffer.append( "<nobr><a href=\"tavern.php?place=susguy\">get free goofballs?</a></nobr><br>" );
		}

		if ( KoLCharacter.getAttacksLeft() > 0 )
		{
			buffer.append( "<nobr><a href=\"peevpee.php?place=fight\">Use remaining PVP fights</a></nobr><br>" );
		}

		ValhallaDecorator.switchSeeds( buffer );

		ValhallaDecorator.switchCorrespondent( buffer );

		ValhallaDecorator.switchWorkshed( buffer );
		
		ValhallaDecorator.switchFolderHolder( buffer );
		
		ValhallaDecorator.checkIceHouse( buffer );

		ValhallaDecorator.switchChateau( buffer );

		ValhallaDecorator.switchCowboyBoots( buffer );
	}

	private static void checkForKeyLime( StringBuffer buffer, int itemId, String keyType )
	{
		if ( !InventoryManager.hasItem( itemId ) )
		{
			return;
		}

		buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=create+" );
		buffer.append( InventoryManager.getAccessibleCount( itemId ) );
		buffer.append( "+" );
		buffer.append( StringUtilities.getURLEncode( keyType ) );
		buffer.append( "+key+lime&pwd=" );
		buffer.append( GenericRequest.passwordHash );
		buffer.append( "\">make a ");
		buffer.append( keyType );
		buffer.append( " key lime</a></nobr><br />" );
	}

	private static final boolean developerGift( final StringBuffer buffer, final int itemId, final String developer )
	{
		int giftCount = InventoryManager.getAccessibleCount( itemId );
		if ( giftCount <= 0 )
		{
			return false;
		}

		String itemName = StringUtilities.getURLEncode( ItemDatabase.getItemName( itemId ) );
		String plural = ItemDatabase.getPluralName( itemId );

		buffer.append( "<nobr><a href=\"/KoLmafia/redirectedCommand?cmd=acquire+" );
		buffer.append( giftCount );
		buffer.append( "+" );
		buffer.append( itemName );
		buffer.append( ";csend+" );
		buffer.append( giftCount );
		buffer.append( "+" );
		buffer.append( itemName );
		buffer.append( "+to+" );
		buffer.append( developer );
		buffer.append( "&pwd=" );
		buffer.append( GenericRequest.passwordHash );
		buffer.append( "\">send your " );
		buffer.append( plural );
		buffer.append( " to " );
		buffer.append( developer );
		buffer.append( "</a></nobr><br>" );
		return true;
	}

	private static final void switchSeeds( final StringBuffer buffer )
	{
		boolean havePumpkin = InventoryManager.hasItem( ItemPool.PUMPKIN_SEEDS );
		boolean havePeppermint = InventoryManager.hasItem( ItemPool.PEPPERMINT_PACKET );
		boolean haveSkeleton = InventoryManager.hasItem( ItemPool.DRAGON_TEETH );
		boolean haveBeer = InventoryManager.hasItem( ItemPool.BEER_SEEDS );
		boolean haveWinter = InventoryManager.hasItem( ItemPool.WINTER_SEEDS );
		if ( !havePumpkin && !havePeppermint && !haveSkeleton && !haveBeer && !haveWinter )
		{
			return;
		}

		buffer.append( "<nobr>Garden: " );

		buffer.append( "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"garden\" onchange=\"if (this.value) window.location.href=this.value\">" );
		buffer.append( "<option value=\"\" style=\"background-color: #eeeeff\">Plant one</option>" );

		if ( havePumpkin )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+pumpkin+seeds;" );
			buffer.append( "+use+packet+of+pumpkin+seeds&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">pumpkin" );
			buffer.append( "</option>" );
		}

		if ( havePeppermint )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=acquire+Peppermint+Pip+Packet;" );
			buffer.append( "+use+Peppermint+Pip+Packet&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">peppermint" );
			buffer.append( "</option>" );
		}

		if ( haveSkeleton )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+dragon's+teeth;" );
			buffer.append( "+use+packet+of+dragon's+teeth&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">skeleton" );
			buffer.append( "</option>" );
		}

		if ( haveBeer )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+beer+seeds;" );
			buffer.append( "+use+packet+of+beer+seeds&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">beer" );
			buffer.append( "</option>" );
		}

		if ( haveWinter )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=acquire+packet+of+winter+seeds;" );
			buffer.append( "+use+packet+of+winter+seeds&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">winter" );
			buffer.append( "</option>" );
		}

		buffer.append( "</select></form>" );

		AdventureResult crop = CampgroundRequest.getCrop();
		if ( crop != null )
		{
			String cropName = crop.getName();
			String cropString = 
				  ( cropName.contains( "peppermint" ) || cropName.contains( "candy cane" ) ) ? "Peppermint"
				: ( cropName.contains( "pumpkin" ) ) ? "Pumpkin"
				: ( cropName.contains( "skeleton" ) ) ? "Skeleton"
				: ( cropName.contains( "barley" ) || cropName.contains( "beer label" ) ) ? "Beer Garden"
				: ( cropName.contains( "ice harvest" ) || cropName.contains( "frost flower" ) ) ? "Winter Garden"
				: "Unknown";
			buffer.append( "</nobr><br><nobr>" );
			buffer.append( " (currently " ).append( cropString ).append( ")" );
		}
		buffer.append( "</nobr><br>" );
	}

	private static final Pattern EUDORA_PATTERN = Pattern.compile( "<option (selected='selected' )?value=\"(\\d)\">([\\w\\s]*)" );
	private static final void switchCorrespondent( final StringBuffer buffer )
	{
		GenericRequest eudoraCheck = new GenericRequest( "account.php?tab=correspondence" );
		eudoraCheck.run();
		String response = eudoraCheck.responseText;
		if ( !response.contains( "Eudora" ) )
		{
			// No choices as tab does not exist
			return;
		}

		// have[Eudora] means that it can be switched to, which means
		// it is not currently active
		boolean havePenpal = false;
		boolean haveGamemag = false;
		boolean haveXi = false;
		String activeEudora = "";
		Matcher matcher = ValhallaDecorator.EUDORA_PATTERN.matcher( response );

		while ( matcher.find() )
		{
			if ( matcher.group(3).equals( "Pen Pal" ) )
			{
				if ( matcher.group(1) == null )
				{
					havePenpal = true;
				}
				else
				{
					activeEudora = "Pen Pal";
				}
			}
			else if ( matcher.group(3).equals( "GameInformPowerDailyPro Magazine" ) )
			{
				if ( matcher.group(1) == null )
				{
					haveGamemag = true;
				}
				else
				{
					activeEudora = "Game Magazine";
				}
			}
			else if ( matcher.group(3).equals( "Xi Receiver Unit" ) )
			{
				if ( matcher.group(1) == null )
				{
					haveXi = true;
				}
				else
				{
					activeEudora = "Xi Receiver";
				}
			}
		}

		if ( !havePenpal && !haveGamemag && !haveXi )
		{
			// No choice to make
			return;
		}

		buffer.append( "<nobr>Eudora: " );

		buffer.append( "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"garden\" onchange=\"if (this.value) window.location.href=this.value\">" );
		buffer.append( "<option value=\"\" style=\"background-color: #eeeeff\">Select one</option>" );

		if ( havePenpal )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=eudora+penpal&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">Pen Pal" );
			buffer.append( "</option>" );
		}
		if ( haveGamemag )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=eudora+game&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">Game Magazine" );
			buffer.append( "</option>" );
		}
		if ( haveXi )
		{
			buffer.append( "<option style=\"background-color: #eeeeff\" " );
			buffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=eudora+xi&pwd=" );
			buffer.append( GenericRequest.passwordHash );
			buffer.append( "\">Xi Receiver" );
			buffer.append( "</option>" );
		}

		buffer.append( "</select></form>" );

		buffer.append( "</nobr><br><nobr>" );
		buffer.append( "(currently " ).append( activeEudora ).append( ")" );
		buffer.append( "</nobr><br>" );
	}

	private static final String shortWorkshedName( String name )
	{
		name = name.replace( "warbear ", "" );
		name = name.replace( "Little Geneticist ", "" );
		return name;
	}

	private static final void switchWorkshed( StringBuffer buffer )
	{
		boolean display = false;
		boolean canChange = !Preferences.getBoolean( "_workshedItemUsed" );
		StringBuilder workshedBuffer = new StringBuilder();

		workshedBuffer.append( "<nobr>Workshed: " );

		if ( canChange )
		{
			workshedBuffer.append( "<form style=\"margin: 0; padding: 0; display: inline;\"><select onchange=\"if (this.value) window.location.href=this.value\">" );
			workshedBuffer.append( "<option value=\"\" style=\"background-color: #eeeeff\">Pick one</option>" );

			for ( int i = 0; i < CampgroundRequest.workshedItems.size(); i++ )
			{
				AdventureResult item = ItemPool.get( CampgroundRequest.workshedItems.get( i ), 1 );
				if ( InventoryManager.hasItem( item ) )
				{
					workshedBuffer.append( "<option style=\"background-color: #eeeeff\" " );
					workshedBuffer.append( "value=\"/KoLmafia/redirectedCommand?cmd=acquire+" );
					String name = item.getName();
					workshedBuffer.append( name.replaceAll( " ", "+" ) );
					workshedBuffer.append( ";+use+" );
					workshedBuffer.append( name.replaceAll( " ", "+" ) );
					workshedBuffer.append( "&pwd=" );
					workshedBuffer.append( GenericRequest.passwordHash );
					workshedBuffer.append( "\">" );
					workshedBuffer.append( ValhallaDecorator.shortWorkshedName( name ) );
					workshedBuffer.append( "</option>" );

					display = true;
				}
			}

			workshedBuffer.append( "</select></form>" );
		}
		else
		{
			workshedBuffer.append( "already changed today" );
		}

		if ( !display && canChange )
		{
			return;
		}

		AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();

		if ( workshedItem != null )
		{
			workshedBuffer.append( "</nobr><br><nobr>" );
			workshedBuffer.append( "(currently " );
			workshedBuffer.append( workshedItem.getName() );
			workshedBuffer.append( ")" );
		}
		workshedBuffer.append( "</nobr><br>" );
		buffer.append( workshedBuffer );
	}

	private static final void switchFolderHolder( StringBuffer buffer )
	{
		StringBuilder folderHolderBuffer = new StringBuilder();

		if ( InventoryManager.getCount( ItemPool.FOLDER_HOLDER ) + InventoryManager.getEquippedCount( ItemPool.FOLDER_HOLDER ) == 0 )
		{
			return;
		}

		folderHolderBuffer.append( "Folder Holder: " );
		for ( int slot = EquipmentManager.FOLDER1; slot <= EquipmentManager.FOLDER3; ++slot )
		{
			AdventureResult folder = EquipmentManager.getEquipment( slot );
			if ( folder != null )
			{
				String name = folder.getName();
				String enchantments;
				if ( name.startsWith( "folder (" ) )
				{
					Modifiers mods = Modifiers.getItemModifiers( folder.getItemId() );
					name = name.substring( 8, name.indexOf( ")" ) );
					enchantments = mods != null ? mods.getString( "Modifiers" ) : "none";
				}
				else
				{
					name = "(empty)";
					enchantments = "none";
				}
				folderHolderBuffer.append( "<nobr><a href=\"inventory.php?action=useholder\" title=\"Change from " );
				folderHolderBuffer.append( enchantments );
				folderHolderBuffer.append( "\">" );
				folderHolderBuffer.append( name );
				folderHolderBuffer.append( "</a></nobr> " );
			}
		}
		folderHolderBuffer.append( "<br>" );
		buffer.append( folderHolderBuffer );
	}

	private static final void checkIceHouse( StringBuffer buffer )
	{
		StringBuilder iceHouseBuffer = new StringBuilder();

		String monster = BanishManager.getIceHouseMonster();

		iceHouseBuffer.append( "<nobr>Ice House: <a href=\"museum.php?action=icehouse\" title=\"Check ice house monster\">" );
		if ( monster != null )
		{
			iceHouseBuffer.append( monster );
			iceHouseBuffer.append( " (currently)</a></nobr>" );
		}
		else
		{
			iceHouseBuffer.append( "(none currently)</a></nobr>" );
		}
		buffer.append( iceHouseBuffer );
	}

	private static final void switchChateau( StringBuffer buffer )
	{
		if ( !Preferences.getBoolean( "chateauAvailable" ) )
		{
			return;
		}

		StringBuilder chateauBuffer = new StringBuilder();

		chateauBuffer.append( "<br>Chateau: " );

		for ( AdventureResult item : KoLConstants.chateau )
		{
			chateauBuffer.append( "<nobr><a href=\"shop.php?whichshop=chateau\" title=\"Change from giving " );
			switch ( item.getItemId() )
			{
			case ItemPool.CHATEAU_MUSCLE:
				chateauBuffer.append( "muscle stats when resting" );
				break;
			case ItemPool.CHATEAU_MYST:
				chateauBuffer.append( "mysticality stats when resting" );
				break;
			case ItemPool.CHATEAU_MOXIE:
				chateauBuffer.append( "moxie stats when resting" );
				break;
			case ItemPool.CHATEAU_FAN:
				chateauBuffer.append( "+5 free rests per day" );
				break;
			case ItemPool.CHATEAU_CHANDELIER:
				chateauBuffer.append( "+3 PvP fights at rollover" );
				break;
			case ItemPool.CHATEAU_SKYLIGHT:
				chateauBuffer.append( "+3 adventures at rollover" );
				break;
			case ItemPool.CHATEAU_BANK:
				chateauBuffer.append( "1,000 meat per day" );
				break;
			case ItemPool.CHATEAU_JUICE_BAR:
				chateauBuffer.append( "3 random potions per day" );
				break;
			case ItemPool.CHATEAU_PENS:
				chateauBuffer.append( "3 fancy calligraphy pens per day" );
				break;
			default:
				chateauBuffer.append( "unknown" );
				break;
			}
			chateauBuffer.append( "\">" );
			chateauBuffer.append( item.getName() );
			chateauBuffer.append( "</a></nobr> " );
		}

		String monster = Preferences.getString( "chateauMonster" );
		chateauBuffer.append( "<br>Chateau monster: " );
		chateauBuffer.append( "<nobr><a href=\"place.php?whichplace=chateau\" title=\"Check painted monster\">" );
		if ( monster.equals( "" ) )
		{
			chateauBuffer.append( "(none currently)" );
		}
		else
		{
			chateauBuffer.append( monster );
			chateauBuffer.append( " (currently)" );
		}
		chateauBuffer.append( "</a></nobr><br>" );

		buffer.append( chateauBuffer );
	}

	private static final void switchCowboyBoots( StringBuffer buffer )
	{
		if ( InventoryManager.getCount( ItemPool.COWBOY_BOOTS ) + InventoryManager.getEquippedCount( ItemPool.COWBOY_BOOTS ) == 0 )
		{
			return;
		}

		StringBuilder cowboyBootsBuffer = new StringBuilder();

		AdventureResult skin = EquipmentManager.getEquipment( EquipmentManager.BOOTSKIN );
		AdventureResult spurs = EquipmentManager.getEquipment( EquipmentManager.BOOTSPUR );

		cowboyBootsBuffer.append( "<nobr>Cowboy Boot skin: " );

		cowboyBootsBuffer.append( "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"cowboy_boots_skin\" onchange=\"if (this.value) window.location.href=this.value\">" );
		cowboyBootsBuffer.append( "<option value=\"\" style=\"background-color: #eeeeff\">Apply one</option>" );

		for ( int i = ItemPool.MOUNTAIN_SKIN; i <= ItemPool.ROTTING_SKIN; i++ )
		{
			if ( InventoryManager.hasItem( i ) && !( skin != null && skin.getItemId() == i ) )
			{
				cowboyBootsBuffer.append( "<option style=\"background-color: #eeeeff\" " );
				cowboyBootsBuffer.append( "title=\"" );
				cowboyBootsBuffer.append( ValhallaDecorator.tooltip( i ) );
				cowboyBootsBuffer.append( "\" value=\"/KoLmafia/redirectedCommand?cmd=acquire+" );
				String name = ItemDatabase.getItemName( i );
				cowboyBootsBuffer.append( name.replaceAll( " ", "+" ) );
				cowboyBootsBuffer.append( ";+use+" );
				cowboyBootsBuffer.append( name.replaceAll( " ", "+" ) );
				cowboyBootsBuffer.append( "&pwd=" );
				cowboyBootsBuffer.append( GenericRequest.passwordHash );
				cowboyBootsBuffer.append( "\">" );
				cowboyBootsBuffer.append( name );
				cowboyBootsBuffer.append( "</option>" );
			}
		}
		cowboyBootsBuffer.append( "</select></form>" );
		
		if ( skin != null && skin != EquipmentRequest.UNEQUIP )
		{
			cowboyBootsBuffer.append( "</nobr><br><nobr>" );
			cowboyBootsBuffer.append( "(currently <span title=\"" );
			cowboyBootsBuffer.append( ValhallaDecorator.tooltip( skin.getItemId() ) );
			cowboyBootsBuffer.append( "\">" );
			cowboyBootsBuffer.append( skin.getName() );
			cowboyBootsBuffer.append( "</span>)" );
		}
		else
		{
			cowboyBootsBuffer.append( "(none currently)" );
		}

		cowboyBootsBuffer.append( "</a></nobr><br>" );

		cowboyBootsBuffer.append( "<nobr>Cowboy Boot spurs: " );

		cowboyBootsBuffer.append( "<form style=\"margin: 0; padding: 0; display: inline;\"><select id=\"cowboy_boots_spurs\" onchange=\"if (this.value) window.location.href=this.value\">" );
		cowboyBootsBuffer.append( "<option value=\"\" style=\"background-color: #eeeeff\">Apply one</option>" );

		for ( int i = ItemPool.QUICKSILVER_SPURS; i <= ItemPool.TICKSILVER_SPURS; i++ )
		{
			if ( InventoryManager.hasItem( i ) && !( spurs != null && spurs.getItemId() == i ) )
			{
				cowboyBootsBuffer.append( "<option style=\"background-color: #eeeeff\" " );
				cowboyBootsBuffer.append( "title=\"" );
				cowboyBootsBuffer.append( ValhallaDecorator.tooltip( i ) );
				cowboyBootsBuffer.append( "\" value=\"/KoLmafia/redirectedCommand?cmd=acquire+" );
				String name = ItemDatabase.getItemName( i );
				cowboyBootsBuffer.append( name.replaceAll( " ", "+" ) );
				cowboyBootsBuffer.append( ";+use+" );
				cowboyBootsBuffer.append( name.replaceAll( " ", "+" ) );
				cowboyBootsBuffer.append( "&pwd=" );
				cowboyBootsBuffer.append( GenericRequest.passwordHash );
				cowboyBootsBuffer.append( "\">" );
				cowboyBootsBuffer.append( name );
				cowboyBootsBuffer.append( "</option>" );
			}
		}
		cowboyBootsBuffer.append( "</select></form>" );
		
		if ( spurs != null && spurs != EquipmentRequest.UNEQUIP )
		{
			cowboyBootsBuffer.append( "</nobr><br><nobr>" );
			cowboyBootsBuffer.append( "(currently <span title=\"" );
			cowboyBootsBuffer.append( ValhallaDecorator.tooltip( spurs.getItemId() ) );
			cowboyBootsBuffer.append( "\">" );
			cowboyBootsBuffer.append( spurs.getName() );
			cowboyBootsBuffer.append( "</span>)" );
		}
		else
		{
			cowboyBootsBuffer.append( "(none currently)" );
		}

		cowboyBootsBuffer.append( "</a></nobr><br>" );
		
		buffer.append( cowboyBootsBuffer );
	}

	private static final String tooltip( int itemId )
	{
		switch( itemId )
		{
		case ItemPool.MOUNTAIN_SKIN:
			return "+50% Moxie";
		case ItemPool.GRIZZLED_SKIN:
			return "+50% Muscle";
		case ItemPool.DIAMONDBACK_SKIN:
			return "+20 Monster Level";
		case ItemPool.COAL_SKIN:
			return "Cowboy Kick does Spooky Damage";
		case ItemPool.FRONTWINDER_SKIN:
			return "+50% Mysticality";
		case ItemPool.ROTTING_SKIN:
			return "Cowboy Kick does 15% delevel, plus damage from Cowrruption";
		case ItemPool.QUICKSILVER_SPURS:
			return "+30% Initiative";
		case ItemPool.THICKSILVER_SPURS:
			return "+2 All Elemental Resistance";
		case ItemPool.WICKSILVER_SPURS:
			return "Cowboy Kick does Hot Damage";
		case ItemPool.SLICKSILVER_SPURS:
			return "Cowboy Kick does Sleaze Damage";
		case ItemPool.SICKSILVER_SPURS:
			return "Cowboy Kick does Stench Damage";
		case ItemPool.NICKSILVER_SPURS:
			return "+20% Item Drop";
		case ItemPool.TICKSILVER_SPURS:
			return "+5 Adventures";
		}
		return "";
	}
}
