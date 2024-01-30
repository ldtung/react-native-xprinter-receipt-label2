import { NativeModules } from 'react-native';

type BluetoothPrinter = {
  deviceName: string;
  macAddress: string;
};

type NativeModuleType = typeof NativeModules & {
  RNXprinter: {
    printTcp80mm(
      ip: string,
      port: number,
      payload: string,
    ): Promise<void>;
    printTcp58mm(
      ip: string,
      port: number,
      payload: string,
    ): Promise<void>;
    printBluetooth80mm(
      macAddress: string,
      payload: string,
    ): Promise<void>;
    printBluetooth58mm(
      macAddress: string,
      payload: string,
    ): Promise<void>;
    printUsb80mm(
      payload: string,
      usbDeviceName: string,
    ): Promise<void>;
    printUsb58mm(
      payload: string,
      usbDeviceName: string,
    ): Promise<void>;
    getBluetoothDeviceList(): Promise<BluetoothPrinter[]>;
    getUsbDeviceList(): Promise<string[]>;
    closeTcpConnection(): Promise<boolean>;
    closeBluetoohConnection(): Promise<boolean>;
    closeUsbConnection(): Promise<boolean>;
  };

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
    ): Promise<void>;
    printLabelBluetooth(
      macAddress: string,
      payload: string,
      labelWidth: number,
      labelHeight: number,
      labelGap: number,
      labelSpaceLeft: number,
      labelSpaceTop: number,
    ): Promise<void>;
    printLabelUsb(
      payload: string,
      usbDeviceNameL: string,
      labelWidth: number,
      labelHeight: number,
      labelGap: number,
      labelSpaceLeft: number,
      labelSpaceTop: number,
    ): Promise<void>;
    closeTcpLabelConnection(): Promise<boolean>;
    closeBluetoohLabelConnection(): Promise<boolean>;
    closeUsbLabelConnection(): Promise<boolean>;
  };
};

const { RNXprinter }: NativeModuleType =
  NativeModules as NativeModuleType;

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
};

const getConfig = (
  args: Partial<typeof defaultConfig>
): typeof defaultConfig => {
  return Object.assign({}, defaultConfig, args);
};

const printTcp80mm = async (
  args: Partial<PrintTcpInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    ip,
    port,
    payload,
  } = getConfig(args);

  await RNXprinter.printTcp80mm(
    ip,
    port,
    payload,
    );
  };

const printTcp58mm = async (
  args: Partial<PrintTcpInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    ip,
    port,
    payload,
  } = getConfig(args);

  await RNXprinter.printTcp58mm(
    ip,
    port,
    payload,
    );
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
    );
  };

const printBluetooth80mm = (
  args: Partial<PrintBluetoothInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    macAddress,
    payload,
  } = getConfig(args);

  return RNXprinter.printBluetooth80mm(
    macAddress,
    payload,
  );
};

const printBluetooth58mm = (
  args: Partial<PrintBluetoothInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    macAddress,
    payload,
  } = getConfig(args);

  return RNXprinter.printBluetooth58mm(
    macAddress,
    payload,
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
  } = getConfig(args);

  return RNXprinterLabel.printLabelBluetooth(
    macAddress,
    payload,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
  );
};

const printUsb80mm = (
  args: Partial<PrintBluetoothInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    payload,
    usbDeviceName,
  } = getConfig(args);

  return RNXprinter.printUsb80mm(
    payload,
    usbDeviceName,
  );
};

const printUsb58mm = (
  args: Partial<PrintBluetoothInterface> & Pick<PrinterInterface, 'payload'>
): Promise<void> => {
  const {
    payload,
    usbDeviceName,
  } = getConfig(args);

  return RNXprinter.printUsb58mm(
    payload,
    usbDeviceName,
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
  } = getConfig(args);

  return RNXprinterLabel.printLabelUsb(
    payload,
    usbDeviceName,
    labelWidth,
    labelHeight,
    labelGap,
    labelSpaceLeft,
    labelSpaceTop,
  );
};

const getBluetoothDeviceList = (): Promise<BluetoothPrinter[]> => {
  return RNXprinter.getBluetoothDeviceList();
};

const getUsbDeviceList = (): Promise<string[]> => {
  return RNXprinter.getUsbDeviceList();
};

const closeTcpConnection = (): Promise<boolean> => {
  return RNXprinter.closeTcpConnection();
};

const closeBluetoohConnection = (): Promise<boolean> => {
  return RNXprinter.closeBluetoohConnection();
};

const closeUsbConnection = (): Promise<boolean> => {
  return RNXprinter.closeUsbConnection();
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
  printTcp80mm,
  printTcp58mm,
  printBluetooth80mm,
  printBluetooth58mm,
  printUsb80mm,
  printUsb58mm,
  printLabelTcp,
  printLabelBluetooth,
  printLabelUsb,
  defaultConfig,
  getBluetoothDeviceList,
  getUsbDeviceList,
  closeTcpConnection,
  closeBluetoohConnection,
  closeUsbConnection,
  closeTcpLabelConnection,
  closeBluetoohLabelConnection,
  closeUsbLabelConnection,
};

