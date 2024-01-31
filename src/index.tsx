import { NativeModules } from 'react-native';

type NativeModuleType = typeof NativeModules & {
  RNXprinterLabel: {
    printLabelTcp(
      ip: string,
      port: number,
      payload: string,
      labelWidth: number,
      labelHeight: number,
      labelGap: number,
      labelSpaceLeft: number,
      labelSpaceTop: number,
      closeAfterPrinted: boolean,
    ): Promise<void>;
    printLabelBluetooth(
      macAddress: string,
      payload: string,
      labelWidth: number,
      labelHeight: number,
      labelGap: number,
      labelSpaceLeft: number,
      labelSpaceTop: number,
      closeAfterPrinted: boolean,
    ): Promise<void>;
    printLabelUsb(
      payload: string,
      usbDeviceNameL: string,
      labelWidth: number,
      labelHeight: number,
      labelGap: number,
      labelSpaceLeft: number,
      labelSpaceTop: number,
      closeAfterPrinted: boolean,
    ): Promise<void>;
    closeTcpLabelConnection(): Promise<boolean>;
    closeBluetoohLabelConnection(): Promise<boolean>;
    closeUsbLabelConnection(): Promise<boolean>;
  };
};

const { RNXprinterLabel }: NativeModuleType =
  NativeModules as NativeModuleType;

interface PrinterInterface {
  payload: string;
  usbDeviceName: string;
  labelWidth: number,
  labelHeight: number,
  labelGap: number,
  labelSpaceLeft: number,
  labelSpaceTop: number,
  closeAfterPrinted: boolean,
}

interface PrintTcpInterface extends PrinterInterface {
  ip: string;
  port: number;
}

interface PrintBluetoothInterface extends PrinterInterface {
  macAddress: string;
}

let defaultConfig: PrintTcpInterface & PrintBluetoothInterface = {
  macAddress: '',
  ip: '192.168.192.168',
  port: 9100,
  payload: '',
  usbDeviceName: '',
  labelWidth: 50,
  labelHeight: 30,
  labelGap: 2,
  labelSpaceLeft: 0,
  labelSpaceTop: 6,
  closeAfterPrinted: true,
};

const getConfig = (
  args: Partial<typeof defaultConfig>
): typeof defaultConfig => {
  return Object.assign({}, defaultConfig, args);
};

const printLabelTcp = async (
  args: Partial<PrintTcpInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    ip,
    port,
    payload,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
    closeAfterPrinted,
  } = getConfig(args);

  await RNXprinterLabel.printLabelTcp(
    ip,
    port,
    payload,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
    closeAfterPrinted,
    );
  };

const printLabelBluetooth = (
  args: Partial<PrintBluetoothInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    macAddress,
    payload,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
    closeAfterPrinted,
  } = getConfig(args);

  return RNXprinterLabel.printLabelBluetooth(
    macAddress,
    payload,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
    closeAfterPrinted,
  );
};

const printLabelUsb = (
  args: Partial<PrintBluetoothInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    payload,
    usbDeviceName,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
    closeAfterPrinted,
  } = getConfig(args);

  return RNXprinterLabel.printLabelUsb(
    payload,
    usbDeviceName,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
    closeAfterPrinted,
  );
};

const closeTcpLabelConnection = (): Promise<boolean> => {
  return RNXprinterLabel.closeTcpLabelConnection();
};

const closeBluetoohLabelConnection = (): Promise<boolean> => {
  return RNXprinterLabel.closeBluetoohLabelConnection();
};

const closeUsbLabelConnection = (): Promise<boolean> => {
  return RNXprinterLabel.closeUsbLabelConnection();
};

export default {
  printLabelTcp,
  printLabelBluetooth,
  printLabelUsb,
  defaultConfig,
  closeTcpLabelConnection,
  closeBluetoohLabelConnection,
  closeUsbLabelConnection,
};

