
#define MAINAPP

#include "main.h"



const unsigned char bootloaderNSName[] = "neuronrobotics.bootloader.*;0.3;;\0";

const unsigned char infoNSName[] = "neuronrobotics.dyio._rev;0.3;;\0";



void BlinkUSBStatus(void);
boolean DebugFlag = true;
//unsigned int __attribute__((section("boot_software_key_sec,\"aw\",@nobits#"))) SoftwareKey;

#define SYS_FREQ 			(80000000L)

static BowlerPacket Packet;

//#define isPressed() (_RB0==1)
//#define initButton() (_TRISB0)=1;

int main(void) {
    InitLEDS();
    SetColor(1, 1, 1);
    initButton();
    int timeout = 0;
    while (isPressed()) {
        Delay10us(1);
        timeout++;
        if (timeout >= 10000) {
            BlinkUSBStatus();
        }
    }
    SetColor(0, 0, 0);
    if ((!isPressed() && (timeout < 10000))) {

        void (*fptr)(void);
        fptr = (void (*)(void))StartAppVectVirtual;
        // If there IS no real program to execute, then fall through to the bootloader
        if (*(int *) StartAppVectVirtual != 0xFFFFFFFF) {
            StopSPI();
            fptr();
            while (1);
        }
    }

    InitializeSystem();
    
    println_I("Stack initialized");
    while (1) {
        BlinkUSBStatus();
        if (getVendorCode() != 0x1E) {
            SetColor(1, 0, 0);
        }
        Bowler_Server(&Packet, true);
        if ((isPressed() || getBootloaderResetFlag())) {
            println_E("Rebooting "); p_int_E(isPressed());print_E(", ");p_int_E(getBootloaderResetFlag());
            U1CON = 0x0000;
            DelayMs(100);
            Reset();
        }
    }//end while
}
static uint8_t avrID[7];

void InitializeSystem(void) {
    Bowler_Init();
    EnableDebugTerminal();
    setPrintLevelInfoPrint();
    clearPrint();

#if !defined(MAJOR_REV)
#define MAJOR_REV			3
#define MINOR_REV			0
#define FIRMWARE_VERSION	1
#endif
    //uint8_t rev[] = {MAJOR_REV, MINOR_REV, FIRMWARE_VERSION};
    //println_I("Seting BL version");
    //FlashSetBlRev(rev);
    
    println_I("Adding Namespaces ");
    addNamespaceToList(get_bcsBootloaderNamespace());

    //println_I("Namespaces added");

    char * dev = "Bootloader";
    char ser[13];
    int i;
    char tmp;
    for (i = 0; i < 6; i++) {
        if (i % 2) {
            tmp='0';
        } else {
            tmp='F';
        }
        ser[i*2] = tmp;
        ser[1+i*2 ] = tmp;
    }
    ser[12]=0;

    Pic32_Bowler_HAL_Init();
    usb_CDC_Serial_Init(dev, ser, 0x04D8, 0x0001);



#if defined(DYIO)
    InitAVR_RST();
    InitSPI();
    

    SetColor(1, 0, 1);
    programMode();
    writeLowFuse();
    writeHighFuse();
    writeExtendedFuse();
    GetAVRid(avrID);

    ReleaseAVRReset();
    DelayMs(10);
    HoldAVRReset();
    GetAVRid(avrID);
#else
#error No Bootloader type defined
#endif

}



