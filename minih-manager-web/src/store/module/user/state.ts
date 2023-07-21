import { UserState} from "./user-types";

export const userState: Partial<UserState> = {
    token: "",
    rawToken: "",
    sessionInfo: {},
    userInfo: {name:"",avatar:""}
}
