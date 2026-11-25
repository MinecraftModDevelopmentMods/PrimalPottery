package net.modcrafters.minecraft.primalpottery.pottery;

public record PotteryDisc(double height, double radius, double innerRadius) {
    public PotteryDisc(double height, double radius) {
        this(height, radius, 0);
    }
}
