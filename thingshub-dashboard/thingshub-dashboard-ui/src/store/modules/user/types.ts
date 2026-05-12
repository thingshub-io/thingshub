export type RoleType = '' | '*' | 'admin' | 'user';
export type Role = {
  id: number;
  name: string;
  permission: {
    name: string;
    desc: string;
    id: number;
  }[];
  menus: { id: number; name: string }[];
};
export interface UserInfo {
  id: string;
  name: string;
  email: string;
  department?: string;
  employeeType?: string;
  job?: string;
  probationStart?: string;
  probationEnd?: string;
  probationDuration?: string;
  protocolStart?: string;
  protocolEnd?: string;
  address?: string;
  status?: string;
  role: RoleType;
  updateTime?: any;
  createTime?: any;
  roleId?: number;
  rolePermission?: string[];
}
export interface UserFilterData {
  sort?: number;
  startTime?: string;
  endTime?: string;
  filterStatus?: Array<string>;
  filterType?: Array<string>;
  submit?: boolean;
  reset?: boolean;
}
export type UserState = UserInfo & UserFilterData;

export type DataOption = {
  id: string;
  name: string;
};
export type UserProfile = {
  id: string;
  name: string;
  nick: string;
  mobile: string;
  avatar: string;
  type: number;
  status: number;
  tenantId: string;
  tenantName: string;
  adminFlag: number;
  orgs: {
    id: string;
    name: string;
  }[];
  tenants: {
    id: string;
    name: string;
  }[];
  serviceItems: {
    appCode: string;
    serviceItemId: string;
    serviceItemName: string;
  }[];
  menus: {
    serviceItemId: string;
    menuId: string;
    menuName: string;
    authorityId: string;
  }[];
};
