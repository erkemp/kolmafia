/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.MonsterDatabase.Monster;

public class AreaCombatData implements KoLConstants
{
	private static float lastDropModifier = 0.0f;
	private static float lastDropMultiplier = 0.0f;

	private int minHit;
	private int maxHit;
	private int minEvade;
	private int maxEvade;

	private int combats;
	private int weights;

	// Parallel lists: monsters and encounter weighting
	private List monsters;
	private List weightings;

	public AreaCombatData( int combats )
	{
		this.monsters = new ArrayList();
		this.weightings = new ArrayList();
		this.combats = combats;
		this.weights = 0;
		this.minHit = Integer.MAX_VALUE;
		this.maxHit = 0;
		this.minEvade = Integer.MAX_VALUE;
		this.maxEvade = 0;
	}

	public boolean addMonster( String name )
	{
		int weighting = 1;

		int colon = name.indexOf( ":" );
		if ( colon > 0 )
		{
			weighting = StaticEntity.parseInt( name.substring( colon + 1 ).trim() );
			name = name.substring( 0, colon );
		}

		Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
			return false;

		this.monsters.add( monster );
		this.weightings.add( new Integer( weighting ) );

		// Don't let ultra-rare monsters skew hit and evade numbers -
		// or anything else.
		if ( weighting < 0 )
			return true;

		// Don't let special monsters skew combat percentage numbers
		// or things derived from them, like area-wide item and meat
		// drops. Do include them in hit and evade ("safety") numbers.
		if ( weighting > 0 )
			this.weights += weighting;

		int attack = monster.getAttack();
		if ( attack < this.minEvade )
			this.minEvade = attack;
		if ( attack > this.maxEvade )
			this.maxEvade = attack;

		int defense = monster.getDefense();
		if ( defense < this.minHit )
			this.minHit = defense;
		if ( defense > this.maxHit )
			this.maxHit = defense;

		return true;
	}

	public int getMonsterCount()
	{	return this.monsters.size();
	}

	public Monster getMonster( int i )
	{	return (Monster) this.monsters.get(i);
	}

	public int getWeighting( int i )
	{	return ((Integer)this.weightings.get(i)).intValue();
	}

	public int combats()
	{	return this.combats;
	}

	public int minHit()
	{	return this.minHit == Integer.MAX_VALUE ? 0 : this.minHit;
	}

	public int maxHit()
	{	return this.maxHit;
	}

	public int minEvade()
	{	return this.minEvade == Integer.MAX_VALUE ? 0 : this.minEvade;
	}

	public int maxEvade()
	{	return this.maxEvade;
	}

	public boolean willHitSomething()
	{
		int ml = KoLCharacter.getMonsterLevelAdjustment();
				int hitStat = KoLCharacter.getAdjustedHitStat();

		return hitPercent( hitStat - ml, this.minHit() ) > 0.0f;
	}

	public String toString()
	{	return toString( false );
	}

	public String toString( boolean fullString )
	{
		int ml = KoLCharacter.getMonsterLevelAdjustment();
		int moxie = KoLCharacter.getAdjustedMoxie() - ml;

		String statName = KoLCharacter.hitStat() == MOXIE ? "Mox" : "Mus";
		int hitstat = KoLCharacter.getAdjustedHitStat() - ml;

		float minHitPercent = hitPercent( hitstat, this.minHit() );
		float maxHitPercent = hitPercent( hitstat, this.maxHit );
		int minPerfectHit = perfectHit( hitstat, this.minHit() );
		int maxPerfectHit = perfectHit( hitstat, this.maxHit );
		float minEvadePercent = hitPercent( moxie, this.minEvade() );
		float maxEvadePercent = hitPercent( moxie, this.maxEvade );
		int minPerfectEvade = perfectHit( moxie, this.minEvade() );
		int maxPerfectEvade = perfectHit( moxie, this.maxEvade );

		// statGain constants
		FamiliarData familiar = KoLCharacter.getFamiliar();
		float experienceAdjustment = KoLCharacter.getExperienceAdjustment();

		// Area Combat percentage
		float combatFactor = this.areaCombatPercent() / 100.0f;

		// Iterate once through monsters to calculate average statGain
		float averageExperience = 0.0f;

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			int weighting = this.getWeighting( i );

			// Omit ultra-rare (-1) and special (0) monsters
			if ( weighting < 1 )
				continue;

			Monster monster = this.getMonster( i );
			float weight = (float)weighting / (float)this.weights;
			averageExperience += weight * monster.getAdjustedExperience( experienceAdjustment, ml,  familiar );
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append( "<html><b>Hit</b>: " );
		buffer.append( this.getRateString( minHitPercent, minPerfectHit, maxHitPercent, maxPerfectHit, statName, fullString ) );

		buffer.append( "<br><b>Evade</b>: " );
		buffer.append( this.getRateString( minEvadePercent, minPerfectEvade, maxEvadePercent, maxPerfectEvade, "Mox", fullString ) );
		buffer.append( "<br><b>Combat Rate</b>: " );

		if ( this.combats > 0 )
		{
			buffer.append( this.format( combatFactor * 100.0f ) + "%" );
			buffer.append( "<br><b>Combat XP</b>: " + FLOAT_FORMAT.format( averageExperience * combatFactor ) );
		}
		else if ( this.combats == 0 )
			buffer.append( "0%" );
		else
			buffer.append( "No data" );

		for ( int i = 0; i < this.monsters.size(); ++i )
		{
			buffer.append( "<br><br>" );
			buffer.append( this.getMonsterString( this.getMonster( i ), moxie, hitstat, ml, this.getWeighting( i ), combatFactor, fullString ) );
		}

		buffer.append( "</html>" );
		return buffer.toString();
	}

	private String format( float percentage )
	{	return String.valueOf( (int) percentage );
	}

	private float areaCombatPercent()
	{
		// If we don't have the data, pretend it's all combat
		if ( this.combats < 0 )
			return 100.0f;

		// Some areas are inherently all combat or no combat
		if ( this.combats == 0 || this.combats == 100 )
			return this.combats;

		float pct = this.combats + KoLCharacter.getCombatRateAdjustment();
		return Math.max( 0.0f, Math.min( 100.0f, pct ) );
	}

	private String getRateString( float minPercent, int minMargin, float maxPercent, int maxMargin, String statName, boolean fullString )
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append( this.format( minPercent ) );
		buffer.append( "%/" );

		buffer.append( this.format( maxPercent ) );
		buffer.append( "%" );

		if ( !fullString )
			return;

		buffer.append( " (" );

		buffer.append( statName );

		if ( minMargin >= 0 )
			buffer.append( "+" );
		buffer.append( minMargin );

		buffer.append( "/" );

		if ( maxMargin >= 0 )
			buffer.append( "+" );
		buffer.append( maxMargin );

		buffer.append( ")" );
		return buffer.toString();
	}

	private String getMonsterString( Monster monster, int moxie, int hitstat, int ml, int weighting, float combatFactor, boolean fullString )
	{
		// moxie and hitstat already adjusted for monster level

		int defense = monster.getDefense();
		float hitPercent = hitPercent( hitstat, defense );
		int perfectHit = perfectHit( hitstat, defense );

		int attack = monster.getAttack();
		float evadePercent = hitPercent( moxie, attack );
		int perfectEvade = perfectHit( moxie, attack );

		int health = monster.getAdjustedHP( ml );
		float statGain = monster.getExperience();

		StringBuffer buffer = new StringBuffer();

		int ed = monster.getDefenseElement();
		int ea = monster.getAttackElement();
		int element = ( ed == MonsterDatabase.NONE ) ? ea : ed;

		// Color the monster name according to its element
		buffer.append( " <font color=" + elementColor( element ) + "><b>" );
		buffer.append( monster.getName() );
		buffer.append( "</b></font> (" );

		if ( weighting < 0 )
		{
			buffer.append( "ultra-rare" );
		}
		else if ( weighting == 0 )
		{
			buffer.append( "special" );
		}
		else
		{
			buffer.append( this.format( 100.0f * combatFactor * weighting / this.weights ) + "%" );
				buffer.append( ")<br>Hit: <font color=" + elementColor( ed ) + ">" );
		}

		buffer.append( this.format( hitPercent ) );
		buffer.append( "%</font>, Evade: <font color=" + elementColor( ea ) + ">" );
		buffer.append( this.format( evadePercent ) );
		buffer.append( "%</font><br>HP: " + health + ", XP: " + FLOAT_FORMAT.format( statGain ) );

		if ( fullString )
			this.appendMeatDrop( buffer, monster );

		this.appendItemList( buffer, monster.getItems(), monster.getPocketRates(), fullString );

		return buffer.toString();
	}

	private void appendMeatDrop( StringBuffer buffer, Monster monster )
	{
		int minMeat = monster.getMinMeat();
		int maxMeat = monster.getMaxMeat();
		if ( minMeat == 0 && maxMeat == 0 )
			return;

		float modifier = ( KoLCharacter.getMeatDropPercentAdjustment() + 100.0f ) / 100.0f;
		buffer.append( "<br>Meat: " +
		   this.format( (minMeat) * modifier ) + "-" + this.format( (maxMeat) * modifier ) + " (" +
		   this.format( (minMeat + maxMeat) * modifier / 2.0f ) + " average)" );
	}

	private void appendItemList( StringBuffer buffer, List items, List pocketRates, boolean fullString )
	{
		if ( items.size() == 0 )
			return;

		float itemModifier = getDropRateModifier();

		for ( int i = 0; i < items.size(); ++i )
		{
			AdventureResult item = (AdventureResult) items.get(i);

			if ( !fullString )
			{
				if ( i == 0 )
					buffer.append( "<br>" );
				else
					buffer.append( ", " );

				buffer.append( item.getName() );
				continue;
			}

			buffer.append( "<br>" );

			float stealRate = KoLCharacter.isMoxieClass() && !KoLCharacter.canInteract() ? ((Float) pocketRates.get(i)).floatValue() : 0.0f;
			float dropRate = Math.min( (item.getCount()) * itemModifier, 100.0f );
			float effectiveDropRate = (stealRate * 100.0f) + ((1.0f - stealRate) * dropRate);

			String rate1 = this.format( dropRate );
			String rate2 = this.format( effectiveDropRate );

			buffer.append( item.getName() );
			buffer.append( " (" );
			buffer.append( rate2 );

			if ( !rate1.equals( rate2 ) )
			{
				buffer.append( "%, " );
				buffer.append( rate1 );
				buffer.append( "%, " );
				buffer.append( this.format( stealRate * 100.0f ) );
			}

			buffer.append( "%)" );
		}
	}

	public static final float getDropRateModifier()
	{
		if ( lastDropMultiplier != 0.0f && KoLCharacter.getItemDropPercentAdjustment() == lastDropModifier )
			return lastDropMultiplier;

		lastDropModifier = KoLCharacter.getItemDropPercentAdjustment();
		lastDropMultiplier = ( 100.0f + lastDropModifier ) / 100.0f;

		return lastDropMultiplier;
	}

	public static final String elementColor( int element )
	{
		if ( element == MonsterDatabase.HEAT )
			return "#ff0000";
		if ( element == MonsterDatabase.COLD )
			return "#0000ff";
		if ( element == MonsterDatabase.STENCH )
			return "#008000";
		if ( element == MonsterDatabase.SPOOKY )
			return "#808080";
		if ( element == MonsterDatabase.SLEAZE )
			return "#8a2be2";

		return "#000000";
	}

	public static final float hitPercent( int attack, int defense )
	{
		// ( (Attack - Defense) / 18 ) * 100 + 50 = Hit%
		float percent = 100.0f * ( attack - defense ) / 18 + 50.0f;
		if ( percent < 0.0f )
			return 0.0f;
		if ( percent > 100.0f )
			return 100.0f;
		return percent;
	}

	public static final int perfectHit( int attack, int defense )
	{	return attack - defense - 9;
	}
}
