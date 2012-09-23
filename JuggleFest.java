import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;

public class JuggleFest
{
	public static void main(String[] args) throws IOException {
		String fileName = args[ 0 ];
		Scanner scanner = new Scanner( new File( fileName ) );

		List< Circuit > circuits = new ArrayList< Circuit >();
		List< Juggler > jugglers = new ArrayList< Juggler >();

		// Parse input file line by line

		try {
			while ( scanner.hasNextLine() ) {
				String[] tokens = scanner.nextLine().split( " " );

				// First store all Circuits

				if ( tokens[ 0 ].equals( "C" ) ) {
					int number = Integer.valueOf( tokens[ 1 ].substring( 1 ) );
					int h = Integer.valueOf( tokens[ 2 ].substring( 2 ) );
					int e = Integer.valueOf( tokens[ 3 ].substring( 2 ) );
					int p = Integer.valueOf( tokens[ 4 ].substring( 2 ) );
					circuits.add( new Circuit( number, h, e, p ) );
				}

				// Then store all Jugglers

				if ( tokens[ 0 ].equals( "J" ) ) {
					int number = Integer.valueOf( tokens[ 1 ].substring( 1 ) );
					int h = Integer.valueOf( tokens[ 2 ].substring( 2 ) );
					int e = Integer.valueOf( tokens[ 3 ].substring( 2 ) );
					int p = Integer.valueOf( tokens[ 4 ].substring( 2 ) );
					String[] prefTokens = tokens[ 5 ].split( "," );
					int[] preferences = new int[ prefTokens.length ];
					int[] scores = new int[ preferences.length ];

					for ( int i = 0; i < preferences.length; ++i ) {
						preferences[ i ] = Integer.valueOf( prefTokens[ i ].substring( 1 ) );
						Circuit circuit = circuits.get( preferences[ i ] );
						scores[ i ] = h * circuit.getH() + e * circuit.getE() + p * circuit.getP(); 
					}
					jugglers.add( new Juggler( number, h, e, p, preferences, scores ) );
				}
			}
		}
		finally {
			scanner.close();
		}

		// Assign all Jugglers to all Circuits

		JCMatcher jcm = new JCMatcher( circuits, jugglers );
		System.out.println( "assigning..." );
		jcm.assign();
		System.out.println( "writing output file..." );
		Writer writer = new OutputStreamWriter( new FileOutputStream( "output.txt" ) );
		try {
			writer.write( jcm.toString() );
		}
		finally {
			writer.close();
		}
	}
}



class JCMatcher
{
	private List< Circuit > _circuits;
	private List< Juggler > _jugglers;	
	private int ncircuits;							// Number of Circuits to be matched
	private int njugglers;							// Number of Jugglers to be matched
	private int njpc;										// Number of Jugglers per Circuit

	/** Constructor */
	public JCMatcher( List< Circuit > circuits, List< Juggler > jugglers )
	{
		_circuits = circuits;
		_jugglers = jugglers;
		ncircuits = circuits.size();
		njugglers = jugglers.size();
		njpc = njugglers / ncircuits;
	}

	/** Method to assign Jugglers to each Circuit */
	public void assign()
	{
		Random random = new Random( System.currentTimeMillis() );
		boolean stop = false;

		// While !stop, iterate over all Jugglers, try to fit them into different circuits
		
		while ( !stop ) {
			stop = true;
			for ( int i = 0; i < njugglers; ++i ) {
				Juggler juggler = _jugglers.get( i );	// Get each Juggler
				
				// Continue if this Juggler is unassigned
				
				if ( !juggler.assigned ) {
					int ind = juggler.index + 1;
					int[] preferences = juggler.getPreferences();	// Get this Juggler's preferences
					int[] scores = juggler.getScores();						// Get scores vs. preference

					// Loop till we've tried all Circuits in preferences
					
					while ( ind < preferences.length ) {
						Circuit circuit = _circuits.get( preferences[ ind ] );	// Get the next Circuit in preferences
						juggler.curcircuit = circuit.getNumber();								// Store its number
						juggler.curscore = scores[ ind ];												// Store the match score
						juggler.index = ind;																		// Update index

						// If the Circuit is not filled, assign this Juggler to it;
						// o.w. if this Juggler has a higher score than the Circuit's
						// minimum score, replace the one with minscore with this Juggler

						if ( circuit.getJugglers().size() < njpc ) {
							juggler.assigned = true;
							circuit.addJuggler( juggler );
							break;
						} else if ( juggler.curscore > circuit.minscore ) {
							Juggler removedJuggler = circuit.getJugglers().remove( njpc - 1 );
							removedJuggler.assigned = false;
							juggler.assigned = true;
							circuit.addJuggler( juggler );
							stop = false;	// When there's unassigned Jugglers, keep looping
							break;
						} else {
							++ind;
						}
					}

					// If the Juggler's still unassigned -> none in preferences is available

					if ( !juggler.assigned ) {

						// Loop till it got assgined a random Circuit

						while ( true ) {
							Circuit ranCircuit = _circuits.get( random.nextInt( 2000 ) );	// Get a random Circuit
							juggler.curcircuit = ranCircuit.getNumber();
							juggler.curscore = juggler.getH() * ranCircuit.getH() + 
																	juggler.getE() * ranCircuit.getE() + 
																	juggler.getP() * ranCircuit.getP();
							
							// If the Circuit is not filled, assign this Juggler to it;
							// o.w. if this Juggler has a higher score than the Circuit's minimum score, 
							// replace the one with mimscore with this Juggler

							if ( ranCircuit.getJugglers().size() < njpc ) {
								juggler.assigned = true;
								ranCircuit.addJuggler( juggler );
								break;
							} else if ( juggler.curscore > ranCircuit.minscore ) {
								Juggler removedJuggler = ranCircuit.getJugglers().remove( njpc - 1 );
								removedJuggler.assigned = false;
								juggler.assigned = true;
								ranCircuit.addJuggler( juggler );
								stop = false;
								break;
							}	// Continue o.w.
						}
					}
				}
			}
		}
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String nl = System.getProperty("line.separator");

		for ( int i = _circuits.size() - 1; i >= 0; --i ) {
			Circuit circuit = _circuits.get( i );
			sb.append( circuit + nl );
		}

		return sb.toString();
	}
}



class Circuit
{
	private int _number;							// The number of this Circuit /starting from 0/
	private int _h;
	private int _e;
	private int _p;
	private List< Juggler > jugglers;	// The Jugglers assigned to this Circuit

	public int minscore;							// The minimum match score among all member jugglers

	/** Getters */
	public int getNumber() { return _number; }
	public int getH() { return _h; }
	public int getE() { return _e; }
	public int getP() { return _p; }
	public List< Juggler > getJugglers() { return jugglers; }

	/** Constructor */
	public Circuit( int number, int h, int e, int p )
	{
		_number = number;
		_h = h;
		_e = e;
		_p = p;
		jugglers = new ArrayList< Juggler >();
		minscore = Integer.MAX_VALUE;
	}

	/** Method for adding member Jugglers */
	public void addJuggler( Juggler juggler )
	{
		jugglers.add( juggler );
		sortJugglers();
		minscore = jugglers.get( jugglers.size() - 1 ).curscore;
	}

	/** Method for sorting member Jugglers according to their match scores */
	private void sortJugglers()
	{
		Collections.sort( jugglers, new Comparator< Juggler >() {
			public int compare( Juggler a, Juggler b )
			{
				// Sort in reverse order.
				if ( a.curscore < b.curscore ) return 1;
				if ( a.curscore == b.curscore ) return 0;
				return -1;
			}
		});
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "C" + _number + " " );

		for ( int i = 0; i < jugglers.size() - 1; ++i ) {
			sb.append( jugglers.get( i ) + "," );
		}
		sb.append( jugglers.get( jugglers.size() - 1 ) );

		return sb.toString();
	}
}



class Juggler
{
	private int _number;				// The number of this Juggler /starting from 0/
	private int _h;
	private int _e;
	private int _p;
	private int[] _preferences;	// The numbers of this Juggler's preference Circuits
	private int[] _scores;			// The match scores for _preferences
	
	public boolean assigned;		// A flag indicating this Juggler is assigned a Circuit
	public int curcircuit;			// The number of the Circuit we'd like to assign this Juggler to
	public int curscore;				// The match score of for that Circuit
	public int index;						// The index of the current Circuit in _preferences

	/** Getters */
	public int getNumber() { return _number; }
	public int getH() { return _h; }
	public int getE() { return _e; }
	public int getP() { return _p; }
	public int[] getPreferences() { return _preferences; }
	public int[] getScores() { return _scores; }

	/** Constructor */
	public Juggler( int number, int h, int e, int p, int[] preferences, int[] scores )
	{
		_number = number;
		_h = h;
		_e = e;
		_p = p;
		_preferences = ( int[] ) preferences.clone();
		_scores = ( int[] ) scores.clone();
		assigned = false;
		curcircuit = -1;
		curscore = -1;
		index = -1;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append( "J" + _number );
		
		for ( int i = 0; i < _preferences.length; ++i ) {
			sb.append( " C" + _preferences[i] + ":" + _scores[i] );
		}

		return sb.toString();
	}
}