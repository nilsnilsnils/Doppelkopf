package nldoko.game.XML;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import nldoko.android.Functions;
import nldoko.game.R;
import nldoko.game.classes.GameClass;
import nldoko.game.classes.PlayerClass;
import nldoko.game.classes.RoundClass;
import nldoko.game.data.DokoData;
import nldoko.game.data.DokoData.GAME_CNT_VARIANT;
import nldoko.game.data.DokoData.GAME_ROUND_RESULT_TYPE;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;


public class DokoXMLClass {

    public static final String APP_DIR = "DokoApp";
    public static final String APP_DIR_GAMES = "DokoApp/games";
    public static final String SAVED_GAME_FILE_SUFFIX =  "_dokoSavedGame.xml";

	private static final String TAG = "DokoXMLClass";

    private static final String GAME_XML_STRUCT_VERSION = "2";
    private static final String GAME_XML_STRUCT_VERSION_ATTR = "version";

    private static final String GAME_SETTINGS_TAG           = "GamSettings";
    private static final String GAME_SETTINGS_PLAYER_COUNT  = "PlayerCnt";
    private static final String GAME_SETTINGS_ACTIVE_PLAYERS = "ActivePlayers";
    private static final String GAME_SETTINGS_BOCK_ROUND_LIMIT = "BockRoundLimit";
    private static final String GAME_SETTINGS_BOCK_AUTO_CALC = "BockAutoCalc";
    private static final String GAME_SETTINGS_COUNT_VARIANT = "GameCntVariant";
    private static final String GAME_SETTINGS_MARK_SUSPENDED_PLAYERS = "MarkSuspendedPlayers";

    private static final String GAME_CREATE_DATE = "CreateDate";
    private static final String GAME = "Game";

    private static final String GAME_ROUNDS = "Rounds";
    private static final String GAME_ROUND = "Round";
    private static final String GAME_ROUND_ID = "RoundID";
    private static final String GAME_ROUND_POINTS = "RoundPoints";
    private static final String GAME_ROUND_BOCK_CNT = "RoundBockCount";
    private static final String GAME_ROUND_TYPE = "RoundType";

    private static final String GAME_PLAYERS = "Players";
    private static final String GAME_PLAYER = "Player";
    private static final String GAME_PLAYER_ID = "PlayerID";
    private static final String GAME_PLAYER_NAME = "name";
    private static final String GAME_PLAYER_POINTS = "points";
    private static final String GAME_PLAYER_POINT_HISTORY = "PointsHistory";
    private static final String GAME_PLAYER_POINT_HISTORY_ROUND = "Round";
    private static final String GAME_PLAYER_POINT_HISTORY_POINTS = "Points";
    private static final String GAME_PLAYER_POINT_HISTORY_POINTS_AT_ROUND = "PointAtRound";

    private static final String GAME_PRE_ROUNDS = "PreRounds";
    private static final String GAME_PRE_ROUND = "PreRound";
    private static final String GAME_PRE_ROUND_BOCK_COUNT = "bockCount";

    private static final String PLAYER_NAMES_NAMES = "Names";
    private static final String PLAYER_NAMES_NAME = "name";

	public static boolean saveGameStateToXML(Context c, GameClass game){
		if(game == null) {
            // no game ?!
            return false;
        }

        // prepare file handling
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        String oldGameFile = game.currentFilename();
        String newFilename = game.generateNewFilename();

        File finalFile = null;

        // try to save on external storage
        File externalStorage = getExternalStorageDir(true);

        if (externalStorage != null) {
            boolean dirReady = createAppDirsInStorage(externalStorage);

            if (dirReady) {
                finalFile = new File(externalStorage.getAbsolutePath() + File.separatorChar + APP_DIR_GAMES + File.separatorChar + newFilename);

                try {

                    fos = new FileOutputStream(finalFile);
                    osw = new OutputStreamWriter(fos);

                } catch (IOException e) {
                    // Unable to create file, likely because external storage is
                    // not currently mounted.
                    Log.w("ExternalStorage", "Error external storage " + finalFile.getAbsolutePath(), e);
                    fos = null;
                    osw = null;
                }
            }
        }

        // if no external storage or error on external storage use app dir
        if (fos == null || osw == null) {
            //Log.d(TAG,writer.toString());
            if (DokoXMLClass.isAppDirOK(c)) {
                String appDir = getAppDir(c);
                boolean dirReady = createAppDirsInStorage(new File(appDir));

                if (dirReady) {
                    try {

                        finalFile = new File(appDir+newFilename);
                        fos = new FileOutputStream(finalFile);
                        osw = new OutputStreamWriter(fos);

                    } catch (IOException e) {
                        Log.w("ExternalStorage", "Error internal storage ", e);
                        fos = null;
                        osw = null;
                    }
                }
            }
        }

        if (finalFile != null) {
            game.setCurrentFilename(finalFile.getAbsolutePath());
        }

        // XML file content
        if (fos != null && osw != null){
			XmlSerializer serializer = Xml.newSerializer();
		    StringWriter writer = new StringWriter();

    	    try {
    	        serializer.setOutput(writer);
    	        serializer.startDocument("UTF-8", false);
    	        serializer.text("\n");
    	        serializer.startTag("", GAME);
                serializer.attribute("", GAME_XML_STRUCT_VERSION_ATTR, GAME_XML_STRUCT_VERSION);

                // only @ version > 2.5
                // add game settings
                addGameSettingsToXML(serializer, game);

                // add played rounds
                serializer.text("\n\t");
                serializer.startTag("", GAME_ROUNDS);
                ArrayList<RoundClass> rounds = game.getRoundList();
                for(int i=0; i < rounds.size(); i++){
                    RoundClass r = rounds.get(i);

                    serializer.text("\n\t\t");
                    serializer.startTag("", GAME_ROUND);

                    serializer.text("\n\t\t\t");
                    serializer.startTag("", GAME_ROUND_ID);
                    serializer.text(Integer.toString(r.getID()));
                    serializer.endTag("", GAME_ROUND_ID);

                    serializer.text("\n\t\t\t");
                    serializer.startTag("", GAME_ROUND_TYPE);
                    serializer.text(GAME_ROUND_RESULT_TYPE.stringValueOf(r.getRoundType()));
                    serializer.endTag("", GAME_ROUND_TYPE);

                    serializer.text("\n\t\t\t");
                    serializer.startTag("", GAME_ROUND_POINTS);
                    int mBockMulti = 1;
                    if (r.getBockCount() > 0) {
                        mBockMulti = r.getBockCount() * 2;
                    }
                    serializer.text(Integer.toString(r.getPoints() / mBockMulti));
                    serializer.endTag("", GAME_ROUND_POINTS);

                    serializer.text("\n\t\t\t");
                    serializer.startTag("", GAME_ROUND_BOCK_CNT);
                    serializer.text(Integer.toString(r.getBockCount()));
                    serializer.endTag("", GAME_ROUND_BOCK_CNT);

                    serializer.text("\n\t\t");
                    serializer.endTag("", GAME_ROUND);
                }
                serializer.text("\n\t");
                serializer.endTag("", GAME_ROUNDS);

                // add player infos
    	        serializer.text("\n\t");
    	        serializer.startTag("", GAME_PLAYERS);
    	        for(int i=0;i<game.getMAXPlayerCount();i++){
    	        	PlayerClass p = game.getPlayer(i);

        	        serializer.text("\n\t\t");
        	        serializer.startTag("", GAME_PLAYER);

                    serializer.text("\n\t\t\t");
                    serializer.startTag("", GAME_PLAYER_ID);
                    serializer.text(Integer.toString(p.getID()));
                    serializer.endTag("", GAME_PLAYER_ID);

        	        serializer.text("\n\t\t\t");
    	            serializer.startTag("", GAME_PLAYER_NAME);
    	            serializer.text(p.getName());
    	            serializer.endTag("", GAME_PLAYER_NAME);
    	            
    	            serializer.text("\n\t\t\t");
    	            serializer.startTag("", GAME_PLAYER_POINTS);
    	            serializer.text(Float.toString(p.getPoints()));
    	            serializer.endTag("", GAME_PLAYER_POINTS);

                    // point history
                    serializer.text("\n\t\t\t");
                    serializer.startTag("", GAME_PLAYER_POINT_HISTORY);

                    for (int ph = 0; ph < p.getPointHistoryLength() && ph < p.getPointHistoryAtRoundLength(); ph++) {
                        serializer.text("\n\t\t\t\t");
                        serializer.startTag("", GAME_PLAYER_POINT_HISTORY_ROUND);

                        serializer.text("\n\t\t\t\t\t");
                        serializer.startTag("", GAME_PLAYER_POINT_HISTORY_POINTS);
                        serializer.text(Float.toString(p.getPointHistory(ph)));
                        serializer.endTag("", GAME_PLAYER_POINT_HISTORY_POINTS);

                        serializer.text("\n\t\t\t\t\t");
                        serializer.startTag("", GAME_PLAYER_POINT_HISTORY_POINTS_AT_ROUND);
                        serializer.text(Float.toString(p.getPointHistoryPerRound(ph)));
                        serializer.endTag("", GAME_PLAYER_POINT_HISTORY_POINTS_AT_ROUND);

                        serializer.text("\n\t\t\t\t");
                        serializer.endTag("", GAME_PLAYER_POINT_HISTORY_ROUND);
                    }
                    serializer.text("\n\t\t\t");
                    serializer.endTag("", GAME_PLAYER_POINT_HISTORY);
    	            
    	            serializer.text("\n\t\t");
    	            serializer.endTag("", GAME_PLAYER);
    	        }
    	        serializer.text("\n\t");
	            serializer.endTag("", GAME_PLAYERS);
    	        
	            
    	        serializer.text("\n\t");
    	        serializer.startTag("", GAME_PRE_ROUNDS);
	            for (int t=0;t<game.getPreRoundList().size();t++){
	            	serializer.text("\n\t\t");
		            serializer.startTag("", GAME_PRE_ROUND);
		            
		            serializer.text("\n\t\t\t");
		            serializer.startTag("", GAME_PRE_ROUND_BOCK_COUNT);
		            serializer.text(Integer.toString(game.getPreRoundList().get(t).getBockCount()));
		            serializer.endTag("", GAME_PRE_ROUND_BOCK_COUNT);
		            
		            serializer.text("\n\t\t");
		            serializer.endTag("", GAME_PRE_ROUND);
		        }
    	        serializer.text("\n\t");
	            serializer.endTag("", GAME_PRE_ROUNDS);
	            
    	        serializer.text("\n");
    	        serializer.endTag("", GAME);
    	        serializer.endDocument();
                serializer.flush();
    	        
    	        //Write to file
    	        try{
    	        	//Log.d(TAG,writer.toString());
				    osw.write(writer.toString());
				    osw.flush();
	    		    fos.flush();
	    		    osw.close();
	    		    fos.close();
	    		    if (oldGameFile != null) {
                        File f = new File(oldGameFile);
	    		    	if (f.exists() && !f.isDirectory()) {
                            f.delete();
                        }
	    		    }
	    		    return true;
    	         }	
    	         catch(Exception e){
    	        	 Log.d(TAG,e.toString());
    	         }
    	    } catch (Exception e) {
    	    	Log.d(TAG,e.toString());
    	    } 
    	}
    	return false;
    }

    private static void addGameSettingsToXML(XmlSerializer serializer, GameClass game) {
        if (serializer == null || game == null) {
            return;
        }

        try{
            serializer.text("\n\t");
            serializer.startTag("", GAME_SETTINGS_TAG);

            if (game.getCreateDateTimestamp() != null) {
                serializer.text("\n\t\t");
                serializer.startTag("", GAME_CREATE_DATE);
                serializer.text(game.getCreateDateTimestamp().toString());
                serializer.endTag("", GAME_CREATE_DATE);
            }

            serializer.text("\n\t\t");
            serializer.startTag("", GAME_SETTINGS_PLAYER_COUNT);
            serializer.text(Integer.toString(game.getPlayerCount()));
            serializer.endTag("", GAME_SETTINGS_PLAYER_COUNT);


            serializer.text("\n\t\t");
            serializer.startTag("", GAME_SETTINGS_ACTIVE_PLAYERS);
            serializer.text(Integer.toString(game.getActivePlayerCount()));
            serializer.endTag("", GAME_SETTINGS_ACTIVE_PLAYERS);

            serializer.text("\n\t\t");
            serializer.startTag("", GAME_SETTINGS_BOCK_ROUND_LIMIT);
            serializer.text(Integer.toString(game.getBockRoundLimit()));
            serializer.endTag("", GAME_SETTINGS_BOCK_ROUND_LIMIT);

            serializer.text("\n\t\t");
            serializer.startTag("", GAME_SETTINGS_BOCK_AUTO_CALC);
            serializer.text(Boolean.valueOf(game.isAutoBockCalculationOn()).toString());
            serializer.endTag("", GAME_SETTINGS_BOCK_AUTO_CALC);

            serializer.text("\n\t\t");
            serializer.startTag("", GAME_SETTINGS_COUNT_VARIANT);
            serializer.text(game.getGameCntVariant().toString());
            serializer.endTag("", GAME_SETTINGS_COUNT_VARIANT);

            serializer.text("\n\t\t");
            serializer.startTag("", GAME_SETTINGS_MARK_SUSPENDED_PLAYERS);
            Log.d(TAG,"boolstr:"+Boolean.valueOf(game.isMarkSuspendedPlayersEnable()).toString()+" bool:"+game.isMarkSuspendedPlayersEnable());
            serializer.text(Boolean.valueOf(game.isMarkSuspendedPlayersEnable()).toString());
            serializer.endTag("", GAME_SETTINGS_MARK_SUSPENDED_PLAYERS);

            serializer.text("\n\t");
            serializer.endTag("", GAME_SETTINGS_TAG);

        } catch (Exception e) {
            Log.d(TAG,e.toString());
        }
    }
	
	public static GameClass restoreGameStateFromXML(Context c,String filePath, boolean loadFull) {
		GameClass mGame = new GameClass(filePath);
        if (mGame == null) {
            return null;
        }

		try{
			FileInputStream in = new FileInputStream(filePath);

            //for debug
            FileInputStream in2 = new FileInputStream(filePath);
            String fileContent = convertStreamToString(in2);
            //Log.d(TAG,"File:"+filePath+" content:"+fileContent);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(in);
			doc.getDocumentElement().normalize();
			
			NodeList nodeList = doc.getElementsByTagName(GAME);
            Node mNode = nodeList.item(0);
			
			if(mNode == null){
				Log.d(TAG,"XML Parse Error 1");
				return null;
			}

            //Log.v(TAG, nodeAsText(mNode));

            // check  xml struct version
            String mXMLVersion = null; // null means v1
            if (mNode.getAttributes().getLength() == 1) {
                Node mAttr = mNode.getAttributes().getNamedItem(GAME_XML_STRUCT_VERSION_ATTR);

                if (mAttr != null) {
                    mXMLVersion = mAttr.getTextContent();
                }
            }

            if (mXMLVersion == null || !mXMLVersion.equalsIgnoreCase(GAME_XML_STRUCT_VERSION)) {
                // old version
                DokoXMLClass.setGameSettingsFromNode(mNode, mGame);
            }

            ArrayList<RoundClass> mGameRoundsFromRestore = new ArrayList<RoundClass>();

            NodeList mMainNodes = mNode.getChildNodes();
			for (int i = 0; i < mMainNodes.getLength(); i++) {
                Node n =  mMainNodes.item(i);

                if(n.getNodeType() != Node.ELEMENT_NODE) continue;

                if (n.getNodeName().equalsIgnoreCase(GAME_SETTINGS_TAG)) {
                    // new xml
                    DokoXMLClass.setGameSettingsFromNode(n, mGame);
                }
                else if(n.getNodeName().equalsIgnoreCase(GAME_PLAYERS)){
					DokoXMLClass.setGamePlayersFromNode(n, mGame, loadFull);
				}
                else if(n.getNodeName().equalsIgnoreCase(GAME_PRE_ROUNDS) && loadFull){
                    DokoXMLClass.setGamePreRoundsFromNode(n, mGame);
				}
                else if(n.getNodeName().equalsIgnoreCase(GAME_ROUNDS) && loadFull){
                    mGameRoundsFromRestore = DokoXMLClass.getGameRoundsFromNode(n);
                }
            }

            int historyCnt = mGame.getPlayer(0).getPointHistoryLength();
            if (historyCnt != mGameRoundsFromRestore.size()) {
                // player history different from game
                for (PlayerClass p : mGame.getPlayers()) {
                    p.resetPointHistory();
                }

                ArrayList<RoundClass> rounds = new ArrayList<RoundClass>();
                RoundClass r = new RoundClass(0, GAME_ROUND_RESULT_TYPE.RESTORE_ROUND, 0 , 0);
                rounds.add(r);
                mGame.setRoundList(rounds);
            }
            else {
                if (mGameRoundsFromRestore.size() == historyCnt) {
                    mGame.setRoundList(mGameRoundsFromRestore);
                }
            }


		}
		catch(Exception e){
			Log.d(TAG,e.toString());
			return null;
		}

        if (mGame.getActivePlayerCount() == 0 || mGame.getPlayerCount() == 0) {
            // invalid game
            return null;
        }

		return mGame;
	}

    private static  void setGameSettingsFromNode(Node n, GameClass mGame) {
        if (n == null || mGame == null ) {
            return;
        }

        int mPlayerCnt = 0, mActivePlayers = 0, mBockRoundLimit = 0;
        String mCreateDate = "";
        GAME_CNT_VARIANT mGameCntVariant = GAME_CNT_VARIANT.CNT_VARIANT_NORMAL;
        boolean mMarkSuspendedPlayers = false; // default
        boolean mBockAutoCalc = true; // default

        NodeList mSettingsNodes = n.getChildNodes();

        for (int sn = 0; sn < mSettingsNodes.getLength(); sn++) {
            Node settingsSubNode = mSettingsNodes.item(sn);

            if(settingsSubNode.getNodeType() != Node.ELEMENT_NODE) continue;

            if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_CREATE_DATE))  {
                mCreateDate = settingsSubNode.getTextContent();
                // only version > 2.5
                if (mCreateDate.length() > 0) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
                        java.util.Date parsedDate = dateFormat.parse(mCreateDate);
                        java.sql.Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                        mGame.setCreateDate(timestamp);
                    } catch (Exception e) {
                        Log.d(TAG,"create date error: "+ e.toString());
                    }
                }
            }
            else if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_SETTINGS_PLAYER_COUNT)) {
                mPlayerCnt = Integer.valueOf(settingsSubNode.getTextContent());
                mGame.setPlayerCount(mPlayerCnt);
            }
            else if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_SETTINGS_ACTIVE_PLAYERS)) {
                mActivePlayers = Integer.valueOf(settingsSubNode.getTextContent());
                mGame.setActivePlayerCount(mActivePlayers);
            }
            else if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_SETTINGS_BOCK_ROUND_LIMIT)) {
                mBockRoundLimit = Integer.valueOf(settingsSubNode.getTextContent());
                mGame.setBockRoundLimit(mBockRoundLimit);
            }
            else if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_SETTINGS_BOCK_AUTO_CALC)) {
                mBockAutoCalc = Boolean.valueOf(settingsSubNode.getTextContent());
                mGame.setAutoBockCalculation(mBockAutoCalc);
            }
            else if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_SETTINGS_COUNT_VARIANT)) {
                mGameCntVariant = GAME_CNT_VARIANT.valueOf(settingsSubNode.getTextContent());
                mGame.setGameCntVariant(mGameCntVariant);
            }
            else if(settingsSubNode.getNodeName().equalsIgnoreCase(GAME_SETTINGS_MARK_SUSPENDED_PLAYERS)){
                mMarkSuspendedPlayers = Boolean.valueOf(settingsSubNode.getTextContent());
                mGame.setMarkSuspendedPlayers(mMarkSuspendedPlayers);
            }
        }

    }

    private static void setGamePlayersFromNode(Node n, GameClass mGame, boolean loadPointHistory) {
        if (n == null || mGame == null ) {
            return;
        }

        int mPID = 0;
        Float mPoints = 0.0f;
        String mName = "";
        ArrayList<PlayerClass> mPlayers = new ArrayList<PlayerClass>();
        NodeList mPlayerNodes = n.getChildNodes();

        ArrayList<Float> mPlayerPointsHistoryPoints;
        ArrayList<Float> mPlayerPointsHistoryPointsATRound;

        ArrayList<Float> mBackupPlayerPoints = new ArrayList<Float>();

        for(int t=0; t<mPlayerNodes.getLength();t++) {
            Node mPlayer = mPlayerNodes.item(t);
            if(mPlayer.getNodeType() != Node.ELEMENT_NODE) continue;

            if(mPlayer.getNodeName().equalsIgnoreCase(GAME_PLAYER)){
                NodeList mPlayerValues =  mPlayer.getChildNodes();

                mName = "";
                mPoints = 0.0f;
                mPlayerPointsHistoryPoints = new ArrayList<Float>();
                mPlayerPointsHistoryPointsATRound = new ArrayList<Float>();

                for(int k=0;k<mPlayerValues.getLength();k++){

                    Node mPlayerValue = mPlayerValues.item(k);
                    if(mPlayerValue.getNodeType() != Node.ELEMENT_NODE) continue;
                    if(mPlayerValue.getNodeName().equalsIgnoreCase(GAME_PLAYER_NAME)) {
                        mName = mPlayerValue.getTextContent();
                    }
                    else if(mPlayerValue.getNodeName().equalsIgnoreCase(GAME_PLAYER_POINTS)) {
                        mPoints = Float.valueOf(mPlayerValue.getTextContent());
                        mBackupPlayerPoints.add(mPoints);
                    }
                    else if(mPlayerValue.getNodeName().equalsIgnoreCase(GAME_PLAYER_POINT_HISTORY)) {
                        if (!loadPointHistory) {
                            continue;
                        }

                        NodeList mPlayerValuePoints =  mPlayerValue.getChildNodes();
                        for(int v=0; v<mPlayerValuePoints.getLength();v++) {
                            Node mPlayerPointsHistory = mPlayerValuePoints.item(v);
                            if (mPlayerPointsHistory.getNodeType() != Node.ELEMENT_NODE) continue;

                            NodeList mPlayerValuePointsRound =  mPlayerPointsHistory.getChildNodes();

                            float mPointsCurrent = 0.0f;
                            float mPointsAtRound = 0.0f;
                            int valueCnt = 0;

                            for(int z=0; z<mPlayerValuePointsRound.getLength();z++) {
                                Node mPlayerPointsHistoryRound = mPlayerValuePointsRound.item(z);
                                String nodestring = mPlayerPointsHistoryRound.getTextContent()+" "+ mPlayerPointsHistoryRound.getNodeName();
                                if (mPlayerPointsHistoryRound.getNodeType() != Node.ELEMENT_NODE) continue;

                                if(mPlayerPointsHistoryRound.getNodeName().equalsIgnoreCase(GAME_PLAYER_POINT_HISTORY_POINTS)) {
                                    mPointsCurrent = Float.valueOf(mPlayerPointsHistoryRound.getTextContent());
                                    valueCnt++;
                                }
                                else if(mPlayerPointsHistoryRound.getNodeName().equalsIgnoreCase(GAME_PLAYER_POINT_HISTORY_POINTS_AT_ROUND)) {
                                    mPointsAtRound = Float.valueOf(mPlayerPointsHistoryRound.getTextContent());
                                    valueCnt++;
                                }

                            }

                            if (valueCnt == 2) {
                                mPlayerPointsHistoryPoints.add(mPointsCurrent);
                                mPlayerPointsHistoryPointsATRound.add(mPointsAtRound);
                            }
                        }
                    }


                }

                if(mName.length() > 0){
                    //player found

                    // add round history
                    PlayerClass player;
                    if (mPlayerPointsHistoryPoints.size() > 0 && mPlayerPointsHistoryPointsATRound.size() > 0) {
                        player = new PlayerClass(mPID++,mName,mPlayerPointsHistoryPoints.get(mPlayerPointsHistoryPoints.size() - 1));
                        player.setPointHistoy(mPlayerPointsHistoryPoints, mPlayerPointsHistoryPointsATRound);
                    }
                    else {
                        player = new PlayerClass(mPID++,mName,mPoints);
                        player.updatePoints(0,(float)0);
                    }

                    if (player != null) {
                        mPlayers.add(player);
                    }

                }
            }
        }


        // check if all palyers have same history length otherwise kill
        boolean mCheckHistory = true;
        if (mPlayers != null && mPlayers.size() > 0) {
            int mPointsHistoryPointLength = mPlayers.get(0).getPointHistoryLength();
            int mPointsHistoryPointsATRoundLength = mPlayers.get(0).getPointHistoryAtRoundLength();

            for (PlayerClass p : mPlayers) {
                if (p.getPointHistoryLength() != mPointsHistoryPointLength
                    || p.getPointHistoryAtRoundLength() != mPointsHistoryPointsATRoundLength) {

                    mCheckHistory = false;
                    break;
                }
            }
        }

        if (!mCheckHistory) {
            for (int i = 0; i < mPlayers.size(); i++) {
                PlayerClass p = mPlayers.get(i);
                float startPoints = mBackupPlayerPoints.get(i).floatValue();
                p.resetPointHistoryAndForceStartAndCurrentPointsTo(startPoints);
            }
        }


        //fill inactive players
        int mHistoryCnt = 0;
        if(mPlayers.size() > 0) {
            mHistoryCnt = mPlayers.get(0).getPointHistoryLength();
        }

        for(int i=mPlayers.size();i<DokoData.MAX_PLAYER;i++) {
            PlayerClass pInactive = new PlayerClass(i, "", 0);
            if (mHistoryCnt > 0) {
                // fill points history only 0.0f
                mPlayerPointsHistoryPoints = new ArrayList<Float>();
                mPlayerPointsHistoryPointsATRound = new ArrayList<Float>();
                for (int a = 0; a < mHistoryCnt; a++) {
                    mPlayerPointsHistoryPoints.add(0.0f);
                    mPlayerPointsHistoryPointsATRound.add(0.0f);
                }

                pInactive.setPointHistoy(mPlayerPointsHistoryPoints, mPlayerPointsHistoryPointsATRound);
            }
            mPlayers.add(pInactive);
        }

        // set players in game
        mGame.setPlayers(mPlayers);
    }


    private static void setGamePreRoundsFromNode(Node n, GameClass mGame) {
        if (n == null || mGame == null ) {
            return;
        }

        int mPreID = 1; // 0 = show state
        int mBockCount = -1;

        ArrayList<RoundClass> mPreRounds = new ArrayList<RoundClass>();
        NodeList mPreRoundList = n.getChildNodes();

        for(int t=0; t<mPreRoundList.getLength();t++) {
            Node mPreRound = mPreRoundList.item(t);
            if(mPreRound.getNodeType() != Node.ELEMENT_NODE) continue;

            if(mPreRound.getNodeName().equalsIgnoreCase(GAME_PRE_ROUND)){
                NodeList mPreRoundValues =  mPreRound.getChildNodes();

                for(int k=0;k<mPreRoundValues.getLength();k++){
                    Node mPreRoundValue = mPreRoundValues.item(k);
                    if(mPreRoundValue.getNodeType() != Node.ELEMENT_NODE) continue;
                    if(mPreRoundValue.getNodeName().equalsIgnoreCase(GAME_PRE_ROUND_BOCK_COUNT)) {
                        mBockCount = Integer.valueOf(mPreRoundValue.getTextContent());
                    }

                }
                if(mBockCount != -1)  {
                    mPreRounds.add(new RoundClass(mPreID++, 0, mBockCount));
                }
            }
        }

        // set pre rounds for game
        mGame.setPreRoundList(mPreRounds);
    }

    private static ArrayList<RoundClass> getGameRoundsFromNode(Node n) {
        if (n == null) {
            return null;
        }

        int mRoundID = 0, mRoundBockCount = 0;
        int mRoundPoints = 0;
        GAME_ROUND_RESULT_TYPE mRoundType;

        int valueCnt = 0;

        ArrayList<RoundClass> mRounds = new ArrayList<RoundClass>();
        NodeList mRoundList = n.getChildNodes();

        for(int t=0; t<mRoundList.getLength();t++) {
            Node mRound = mRoundList.item(t);
            if(mRound.getNodeType() != Node.ELEMENT_NODE) continue;

            if(mRound.getNodeName().equalsIgnoreCase(GAME_ROUND)){
                NodeList mRoundValues =  mRound.getChildNodes();
                // reset values
                valueCnt = 0;
                mRoundID = 0;
                mRoundBockCount = 0;
                mRoundPoints = 0;
                mRoundType = GAME_ROUND_RESULT_TYPE.RESTORE_ROUND;

                for(int k=0;k<mRoundValues.getLength();k++){
                    Node mRoundValue = mRoundValues.item(k);
                    if(mRoundValue.getNodeType() != Node.ELEMENT_NODE) continue;


                    if(mRoundValue.getNodeName().equalsIgnoreCase(GAME_ROUND_ID)) {
                        mRoundID = Integer.valueOf(mRoundValue.getTextContent());
                        valueCnt++;
                    }


                    if(mRoundValue.getNodeName().equalsIgnoreCase(GAME_ROUND_TYPE)) {
                        mRoundType = GAME_ROUND_RESULT_TYPE.valueForString(mRoundValue.getTextContent());
                        valueCnt++;
                    }


                    if(mRoundValue.getNodeName().equalsIgnoreCase(GAME_ROUND_POINTS)) {
                        mRoundPoints = Integer.valueOf(mRoundValue.getTextContent());
                        valueCnt++;
                    }


                    if(mRoundValue.getNodeName().equalsIgnoreCase(GAME_ROUND_BOCK_CNT)) {
                        mRoundBockCount = Integer.valueOf(mRoundValue.getTextContent());
                        valueCnt++;
                    }
                }

                if (valueCnt == 4) {
                    // valid round
                    RoundClass round  = new RoundClass(mRoundID, mRoundType, mRoundPoints, mRoundBockCount);
                    mRounds.add(round);
                }

            }
        }

       return mRounds;
    }

    private static String nodeAsText(Node n) {
        return n.getNodeType()+" -"+n.getNodeName()+" - "+n.getNodeValue()+" - "+n.getTextContent();
    }

	public static String getAppDir(Context c){
		return c.getApplicationInfo().dataDir+File.separatorChar;
	}
	
	public static boolean isAppDirOK(Context c){
		File file = new File(getAppDir(c));
		if(!file.isDirectory() || (!file.canWrite() || !file.canRead()) ){
			return false;
		}
		return true;
	}
	
	public static boolean isXMLPresent(Context c,String f,boolean copyXML){
		try {
			c.openFileInput(f);
			return true;
		} catch (FileNotFoundException e) {
			Log.d(TAG,e.toString());
			if(copyXML) return copyXML(c,f);
		}
		return false;
	}
	
	public static boolean copyXML(Context c, String f){
		InputStream ins = c.getResources().openRawResource(R.raw.player_names);
		
		byte[] buffer;
		try {
			buffer = new byte[ins.available()];
			ins.read(buffer);
			ins.close();
			
			FileOutputStream fos = c.openFileOutput(f,Context.MODE_PRIVATE);
			fos.write(buffer);
			fos.close();
			return true;
		} catch (IOException e) {
			Log.d(TAG,e.toString());
			return false;
		}

	}

    private static boolean createAppDirsInStorage(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory() && dir.canRead() && dir.canWrite()) {
            // app dir

            File appDir = new File(dir.getAbsolutePath() + File.separatorChar + APP_DIR + File.separatorChar);
            if(!appDir.exists()) {
                appDir.mkdirs();
                if (!appDir.exists()) {
                    return false;
                }
            }

            // dir for saved games
            File appDirGames = new File(dir.getAbsolutePath() + File.separatorChar + APP_DIR_GAMES + File.separatorChar);
            if(!appDirGames.exists()) {
                appDirGames.mkdirs();
                if (!appDirGames.exists()) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

	public static boolean isExternalStorageReady() {

        boolean mExternalStorageAvailable;
        boolean mExternalStorageWriteable;
        String state = Environment.getExternalStorageState();


        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        if (!((mExternalStorageAvailable) && (mExternalStorageWriteable))) {
            //Toast.makeText(this, "SD card not present", Toast.LENGTH_LONG).show();

        }
        return (mExternalStorageAvailable) && (mExternalStorageWriteable);
    }

    public static String [] getPossibleExternalStorageDirs() {
        String [] sdcardPath = {File.separatorChar + "sdcard1" + File.separatorChar,
                File.separatorChar + "storage" + File.separatorChar + "sdcard1" + File.separatorChar,
                File.separatorChar + "sdcard" + File.separatorChar,
                File.separatorChar + "sdcard0" + File.separatorChar};

        return sdcardPath;
    }

    private static File getExternalStorageDir(boolean mustBeWritable) {
        for (String s : getPossibleExternalStorageDirs()) {
            File sdcard = new File(s);

            if (sdcard.exists() && sdcard.isDirectory() && sdcard.canRead()) {
                if (mustBeWritable && sdcard.canWrite()) {
                    return sdcard;
                } else {
                    return sdcard;
                }
            }
        }

        if (isExternalStorageReady()) {
            return Environment.getExternalStorageDirectory();
        }

        return null;
    }
   
	public static boolean getPlayerNamesFromXML(Context c,String f, ArrayList<String> playerNames){
		Log.d(TAG,"loadFromXML");
	
		Node node;
		NodeList names;

		//InputStream in = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		Document doc;
	
		//http://stackoverflow.com/questions/3448145/xml-file-parsing-in-android

		try {
			//in = new FileInputStream(file);
			
			FileInputStream in = c.openFileInput(f);
			
			db = dbf.newDocumentBuilder();
			doc = db.parse(in);
			doc.getDocumentElement().normalize();
			//Log.d(TAG, ""+configs.getLength() );
			//get MRF node
			NodeList nodeList = doc.getElementsByTagName(PLAYER_NAMES_NAMES);
			node = nodeList.item(0);
			
			if(node == null){
				Log.d(TAG,"XML Parse Error 1");
				return false;
			}
			names = node.getChildNodes();
			playerNames.clear();
			for (int i = 0; i < names.getLength(); i++) {	
				if(names.item(i).getNodeType() != Node.ELEMENT_NODE && !names.item(i).getNodeName().equalsIgnoreCase(PLAYER_NAMES_NAME)){
					continue;
				}
				if(!names.item(i).getTextContent().isEmpty())
					playerNames.add(names.item(i).getTextContent());

			}
			Collections.sort(playerNames);
		} catch (SAXException e) {
			Log.d(TAG, e.toString());
		} catch (IOException e) {
			Log.d(TAG, e.toString());
		} catch (ParserConfigurationException e) {
			Log.d(TAG, e.toString());
		} catch (Exception e){
			Log.d(TAG, e.toString());
		}

		return false;
	}
	
	public static boolean savePlayerNamesToXML(Context c,String file, ArrayList<String> playerNames){
		if(playerNames != null && DokoXMLClass.isAppDirOK(c)){
			XmlSerializer serializer = Xml.newSerializer();
		    StringWriter writer = new StringWriter();
		    try {
		        serializer.setOutput(writer);
		        serializer.startDocument("UTF-8", false);
		        serializer.text("\n");
		        serializer.startTag("", PLAYER_NAMES_NAMES);
		        for (String name: playerNames){
		        	serializer.text("\n");
		        	serializer.text("\t");
		            serializer.startTag("", PLAYER_NAMES_NAME);
		            serializer.text(name);
		            serializer.endTag("", PLAYER_NAMES_NAME);
		        }
		        serializer.text("\n");
		        serializer.endTag("", PLAYER_NAMES_NAMES);
		        serializer.endDocument();
		        try{
	
					/*FileOutputStream fos = c.openFileOutput(f,Context.MODE_PRIVATE);
					fos.write(buffer);
					fos.close();*/
		        	//File f = new File(getAppDir(c)+file);
		        	//f.delete();
	
					FileOutputStream fos = c.openFileOutput(file,Context.MODE_PRIVATE);
					OutputStreamWriter osw = new OutputStreamWriter(fos); 
					
				    osw.write(writer.toString());
				    osw.flush();
	    		    fos.flush();
	    		    osw.close();
	    		    fos.close();
		            return true;
		         }	
		         catch(Exception e){
		        	 Log.d(TAG,e.toString());
		         }
		    } catch (Exception e) {
		    	Log.d(TAG,e.toString());
		    } 
		}
		return false;
	}
	
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static void sendGameViaMail(Context context, GameClass mGame) {
        /* Create the Intent */
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        String text =  context.getResources().getString(R.string.str_saved_game_via_mail_text_1) + " "
                +  mGame.getCreateDate("dd.MM.yyyy - HH:mm")+ "\n\n";

        String csv = "";
        if (mGame != null && mGame.getPlayers().size() > 0 && mGame.getRoundList().size() > 0) {
            // header
            csv += "Nr.,";
            for (int u = 0; u < mGame.getPlayerCount(); u++) {
                PlayerClass p = mGame.getPlayer(u);
                csv += p.getName()+",";
            }
            csv += context.getResources().getString(R.string.str_game_points)+",";
            csv += context.getResources().getString(R.string.str_game_points)+" solo ,";
            csv += context.getResources().getString(R.string.str_bock)+",";
            csv += "\n";


            // rounds
            RoundClass mRound;
            for (int i = 0; i < mGame.getRoundList().size(); i++) {
                mRound = mGame.getRoundList().get(i);

                csv += Integer.toString(mRound.getID())+",";

                for(int u=0; u < mGame.getPlayerCount(); u++) {
                    float mPoints = mGame.getPlayer(u).getPointHistory(mRound.getID());
                    csv += mPoints + ",";
                }

                csv += String.valueOf(mRound.getPoints())+",";
                if (mRound.getRoundType() == GAME_ROUND_RESULT_TYPE.LOSE_SOLO || mRound.getRoundType() == GAME_ROUND_RESULT_TYPE.WIN_SOLO) {
                    csv += String.valueOf(mRound.getPoints()*(mGame.getActivePlayerCount()-1));
                } else {
                    csv += " ,";
                }

                csv += Functions.getBockCountAsRom(mRound.getBockCount())+",";
                csv += "\n";
            }

            csv += "\n\n\n";
            csv += context.getResources().getString(R.string.str_saved_game_via_mail_text_excel);
        }

        text += csv;

        /* Fill it with Data */
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.app_name) + " - " + "App" );
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,text);

        /* Send it off to the Activity-Chooser */
        context.startActivity(Intent.createChooser(emailIntent, context.getResources().getString(R.string.str_saved_game_via_mail_intent)));
    }
}
