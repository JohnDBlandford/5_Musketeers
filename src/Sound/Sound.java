package Sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class Sound {

    public static final int WORLD1_MUSIC = 0;
    public static final int CLUNK = 1;
    public static final int PLAYER_DIES = 2;
    public static final int BATTLE_MUSIC = 3;
    public static final int WORLD2_MUSIC = 4;

    //When adding in a sound found file add the name to the list below
    private static final String[] FILES = {
            "ElevatorMusic", // WORLD1 MUSIC = 0
            "Clunk",         // BATTLE SOUND = 1
            "Player_Die",    //PLAYER DIES = 2
            "Battle_Music",  // MUSIC FOR BATTLES = 3
            "MoreElevator" // WORLD 2 MUSIC = 4
    };

    private Clip clip;

    //Setting a file
    public void setFile(int fileNumber) {
        if (clip != null) {
            clip.close();
        }
        try {
            URL url = getClass().getResource("/Sound/" + FILES[fileNumber] + ".wav");
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(stream);
        } catch (Exception e) {
            System.out.println("FUCKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK");
        }
    }

    //Starts Sound
    public void play(){
        if (clip != null) {
            clip.start();
        }
    }

    //Stops Sound
    public void stop(){
        if (clip != null) {
            clip.stop();
        }
    }

    //Loops Sound
    public void loops(){
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void playOnce(){
        if (clip != null){
            clip.setFramePosition(0);
            clip.start();
        }
    }



}
