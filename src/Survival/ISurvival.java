package Survival;

// this exists so that the reset method isnt duplicated inside of Meter and Survival controller
@FunctionalInterface
public interface ISurvival {
    void reset();
}
