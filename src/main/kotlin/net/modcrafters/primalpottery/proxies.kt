package net.modcrafters.primalpottery

import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.relauncher.Side
import net.ndrei.teslacorelib.BaseProxy

open class CommonProxy(side: Side) : BaseProxy(side) {
    override fun preInit(ev: FMLPreInitializationEvent) {
        super.preInit(ev)

        NetworkRegistry.INSTANCE.registerGuiHandler(PrimalPotteryMod, PrimalPotteryGuiProxy)
    }
}

@Suppress("unused")
class ServerProxy : CommonProxy(Side.SERVER)

@Suppress("unused")
class ClientProxy : CommonProxy(Side.CLIENT)
