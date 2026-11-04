package Sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.net.URL;

public class Sound {

    public static final int WORLD1_MUSIC = 0;
    public static final int CLUNK = 1;
    public static final int PLAYER_DIES = 2;
    public static final int BATTLE_MUSIC = 3;
    public static final int WORLD2_MUSIC = 4;
    public static final int SWORD = 5;

    //When adding in a sound found file add the name to the list below
    private static final String[] FILES = {
            "World1Music", // WORLD1 MUSIC = 0
            "Clunk",         // BATTLE SOUND = 1
            "Player_Die",    // PLAYER DIES = 2
            "Battle_Music",  // MUSIC FOR BATTLES = 3
            "MoreElevator",   // WORLD 2 MUSIC = 4
            "Sword"           // SWORD = 5
    };

    private Clip clip;

    //Setting a file
    public void setFile(int fileName) {
        if (clip != null) {
            clip.close();
        }
        try {
            URL url = getClass().getResource("/Sound/" + FILES[fileName] + ".wav");
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(stream);
        } catch (Exception e) {
            System.out.println("WHY WONT YOU WORK, Cant find file " + fileName);
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

    public void lowerVolume(float volumeLevel){ //WORK IN PROGRESS
        if (clip != null){
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(volumeLevel); // Reduce volume by 10 decibels.
            clip.start();
        }
    }

}
