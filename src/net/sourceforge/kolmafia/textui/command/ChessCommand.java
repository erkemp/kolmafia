/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.RabbitHoleManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChessCommand
	extends AbstractCommand
{
	public ChessCommand()
	{
		this.usage = " load config | board | test | solve - play on the Great Big Chessboard.";
	}

	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "reset" ) )
		{
			RabbitHoleManager.reset();
			return;
		}

		if ( command.equals( "load" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Load what board?" );
				return;
			}

			RabbitHoleManager.load( split[ 1 ], true );
			return;
		}

		if ( command.equals( "board" ) )
		{
			RabbitHoleManager.board();
			return;
		}

		if ( command.equals( "test" ) )
		{
			RabbitHoleManager.test();
			return;
		}

		if ( command.equals( "solve" ) )
		{
			RabbitHoleManager.solve();
			return;
		}

		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "What do you want to do with the chessboard?" );
	}
}
