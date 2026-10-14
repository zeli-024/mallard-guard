package dev.zeli.mallardguard.client;

import dev.zeli.mallardguard.GuardPackets;
import net.minecraft.world.InteractionHand;

/** Local presentation only. Damage and successful parries always come from the server. */
final class GuardPrediction {
    private static final int ACK_TIMEOUT_TICKS=100;
    private long nextId, inputId, startedTick, receivedTick;
    private boolean active, pending, requestedGuard, predicted, shield, block;
    private int perfect;
    private GuardPackets.Status state=new GuardPackets.Status(0,0,0,4,10,false,0,false,0,false);

    long inputId(){return inputId;}
    int toolHandRun(){return state.toolHandRun();}
    InteractionHand lastToolHand(){return state.toolHandRun()==0?null:state.toolOffhand()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;}
    long nextInput(){inputId=++nextId;return inputId;}

    void requestStart(long tick) {
        requestedGuard=pending=true;active=predicted=false;startedTick=receivedTick=tick;
        if(dev.zeli.mallardguard.GuardDiagnostics.enabled(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION))dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION,"Guard request: input="+inputId+", tick="+tick);
    }

    void start(long tick, boolean offhand, boolean shield, boolean parry, boolean block, int perfect, int window, int recharge) {
        this.shield=shield;this.block=block;this.perfect=perfect;
        startedTick=receivedTick=tick;active=pending=requestedGuard=predicted=true;
        state=new GuardPackets.Status(parry?perfect>0?1:2:shield?4:3,0,recharge,window,recharge,offhand,inputId,false,state.toolHandRun(),state.toolOffhand());
    }

    void release(long tick) {
        GuardPackets.Status view=view(tick);
        active=requestedGuard=false;pending=true;startedTick=receivedTick=tick;
        state=new GuardPackets.Status(0,0,view.recharge(),view.window(),view.rechargeMax(),view.offhand(),inputId,false,state.toolHandRun(),state.toolOffhand());
    }

    boolean receive(GuardPackets.Status update, long tick) {
        if(update.inputId()!=inputId){
            if(dev.zeli.mallardguard.GuardDiagnostics.enabled(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION))dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION,"Stale guard reply: received="+update.inputId()+", expected="+inputId);
            return false;
        }
        boolean wasPending=pending;
        pending=false;receivedTick=tick;
        active=requestedGuard && update.phase()!=0 && update.accepted();
        state=update;
        if(wasPending&&dev.zeli.mallardguard.GuardDiagnostics.enabled(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION))dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION,"Guard acknowledgement: input="+inputId+", delayTicks="+(tick-startedTick)+", accepted="+update.accepted()+", phase="+update.phase());
        if(wasPending)dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.GUARD,
            "Guard reply: input="+inputId+", accepted="+update.accepted()+", phase="+update.phase());
        return true;
    }

    GuardPackets.Status view(long tick) {
        if(pending && tick-startedTick>ACK_TIMEOUT_TICKS){
            active=false;pending=false;
            if(dev.zeli.mallardguard.GuardDiagnostics.enabled(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION))dev.zeli.mallardguard.GuardDiagnostics.event(dev.zeli.mallardguard.GuardDiagnostics.PREDICTION,"Guard acknowledgement timed out: input="+inputId+", waitedTicks="+(tick-startedTick));
        }
        int age=(int)Math.clamp(tick-startedTick,0,Integer.MAX_VALUE);
        int elapsed=active?predicted?Math.max(state.elapsed(),age):state.elapsed():0;
        int phase=active?state.phase():0;
        if(predicted && (phase==1 || phase==2)){
            if(elapsed>=state.window())phase=block?shield?4:3:0;
            else phase=elapsed>=perfect?2:1;
        }
        int recharge=(phase==3 || phase==4)?state.recharge():Math.max(0,state.recharge()-(int)Math.clamp(tick-receivedTick,0,Integer.MAX_VALUE));
        return new GuardPackets.Status(phase,elapsed,recharge,state.window(),Math.max(1,state.rechargeMax()),state.offhand(),inputId,state.accepted(),state.toolHandRun(),state.toolOffhand());
    }

    void clear(long tick) {
        nextInput();active=pending=requestedGuard=predicted=false;startedTick=receivedTick=tick;
        state=new GuardPackets.Status(0,0,0,4,10,false,inputId,false,0,false);
    }
}
