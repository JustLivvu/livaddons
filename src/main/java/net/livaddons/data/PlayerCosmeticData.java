package net.livaddons.data;

import java.util.ArrayList;
import java.util.List;

public class PlayerCosmeticData {
    public String uuid;
    public String username;
    public String customNick;
    public String colorStart;
    public String colorEnd;
    public boolean isBold;
    public boolean isItalic;
    public float visualHeight;
    public List<String> cosmetics;

    public PlayerCosmeticData() {
        this.uuid = "";
        this.username = "";
        this.customNick = "";
        this.colorStart = "#FFFFFF";
        this.colorEnd = "#FFFFFF";
        this.isBold = false;
        this.isItalic = false;
        this.visualHeight = 1.0f;
        this.cosmetics = new ArrayList<>();
    }

    public PlayerCosmeticData(String uuid, String username, String customNick, String colorStart, String colorEnd, boolean isBold, boolean isItalic, float visualHeight, List<String> cosmetics) {
        this.uuid = uuid;
        this.username = username;
        this.customNick = customNick;
        this.colorStart = colorStart != null ? colorStart : "#FFFFFF";
        this.colorEnd = colorEnd != null ? colorEnd : "#FFFFFF";
        this.isBold = isBold;
        this.isItalic = isItalic;
        this.visualHeight = visualHeight;
        this.cosmetics = cosmetics != null ? cosmetics : new ArrayList<>();
    }
}
