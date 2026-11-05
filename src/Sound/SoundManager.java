package Sound;


import Screens.PlayLevelScreen.PlayLevelScreenState;

public class SoundManager {

    private static Sound music = new Sound();
    private static int currentMusic = -1;

    public static void playMusic(int fileNumber){
        if(currentMusic == fileNumber){
            return;
        }
        music.play();
        music.setFile(fileNumber);
        music.stop();
        music.loops();
        currentMusic = fileNumber;
    }

    //Turns off music
    public static void stopMusic(){
        music.stop();
        currentMusic = -1;
    }

    //Resumes Music
    public static void resumeMusic(){
        music.play();
        music.loops();
    }

    //Pause Music
    public static void pauseMusic(){
        music.stop();
    }

    //Plays sound once
    public static void playSoundEffect(int fileNumber){
        Sound effect = new Sound();
        effect.setFile(fileNumber);
        effect.playOnce();
    }

    //Changes how loud the volume is. Adding a negative will make it softer. Max is 6f.
    public static void changeVolume(float volumeLevel){
        music.lowerVolume(volumeLevel);
    }


    //Check to see current state of the game or what map to set the background music
    public static void backgroundMusic(String map, PlayLevelScreenState state) {
        if (state == PlayLevelScreenState.BATTLE) { //In a Battle
            changeVolume(3f);
            playMusic(Sound.BATTLE_MUSIC);
        }else if(map == "guild_hall_map"){ //Inside Guild Hall
            changeVolume(-10f); //Lowers volume when inside
            playMusic(Sound.WORLD1_MUSIC);
        } else if (map == "world1") {      //World 1
            changeVolume(3f);
            playMusic(Sound.WORLD1_MUSIC);
        } else if (map == "world2_map") {  //World 2
            changeVolume(3f);
            playMusic(Sound.WORLD2_MUSIC);
        } else {
            stopMusic();
        }
    }
}
