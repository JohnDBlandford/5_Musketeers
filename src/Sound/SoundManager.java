package Sound;

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




}
