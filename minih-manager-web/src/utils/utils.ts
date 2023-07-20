import {checkPassword, checkUsername} from "../api/user";
import {MinihError} from "./http";

export class DateFormat {

    public static format(datetime: Date | string, formatting: string): string {
        let timestamp: Date = datetime as Date;
        if (typeof datetime === 'string') {
            timestamp = new Date(Date.parse(datetime));
        }
        let fullYear: string = timestamp.getFullYear().toString();
        let month: string = timestamp.getMonth().toString();
        let date: string = timestamp.getDate().toString();
        let hours: string = timestamp.getHours().toString();
        let minutes: string = timestamp.getMinutes().toString();
        let seconds: string = timestamp.getSeconds().toString();
        let milliseconds: string = timestamp.getMilliseconds().toString();
        formatting = this.parse(formatting, /[y|Y]+/, fullYear);
        formatting = this.parse(formatting, /[M]+/, month, '00');
        formatting = this.parse(formatting, /[d|D]+/, date, '00');
        formatting = this.parse(formatting, /[h|H]+/, hours, '0');
        formatting = this.parse(formatting, /[m]+/, minutes, '00');
        formatting = this.parse(formatting, /[s]+/, seconds, '00');
        formatting = this.parse(formatting, /[S]+/, milliseconds, '000');
        return formatting;
    }

    private static parse(formatting: string, pattern: RegExp, val: string, min?: string): string {
        while (pattern.test(formatting)) {
            pattern.exec(formatting)?.forEach(value => {
                let length = value.length;
                let valLen = val.length;
                let number = valLen - length;
                let element = val.substring(number);
                if (min) {
                    element = min.substring(element.length) + element;
                }
                formatting = formatting.replace(value, element);
            })
        }
        return formatting;
    }

}

// @ts-ignore
export type Optional<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>

export const idCheck = (_: any, value: any, callback: any) => {
    let city = {
        11: '北京',
        12: '天津',
        13: '河北',
        14: '山西',
        15: '内蒙古',
        21: '辽宁',
        22: '吉林',
        23: '黑龙江',
        31: '上海',
        32: '江苏',
        33: '浙江',
        34: '安徽',
        35: '福建',
        36: '江西',
        37: '山东',
        41: '河南',
        42: '湖北',
        43: '湖南',
        44: '广东',
        45: '广西',
        46: '海南',
        50: '重庆',
        51: '四川',
        52: '贵州',
        53: '云南',
        54: '西藏',
        61: '陕西',
        62: '甘肃',
        63: '青海',
        64: '宁夏',
        65: '新疆',
        71: '台湾',
        81: '香港',
        82: '澳门',
        91: '国外',
    };
    if (!value) {
        callback();
        return
    }
    if (!value || !/^\d{6}(18|19|20)?\d{2}(0[1-9]|1[012])(0[1-9]|[12]\d|3[01])\d{3}(\d|X)$/i.test(value)
    ) {
        callback(new Error('请输入正确的身份证号'));
        return
    } else if (!city[value.slice(0, 2)]) {
        callback(new Error('请输入正确的地址编码'));
        return
    } else {
        if (value.length == 18) {
            value = value.split('');
            let factor = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
            let parity = [1, 0, 'X', 9, 8, 7, 6, 5, 4, 3, 2];
            let sum = 0;
            let ai = 0;
            let wi = 0;
            for (let i = 0; i < 17; i++) {
                ai = value[i];
                wi = factor[i];
                sum += ai * wi;
            }
            console.log(sum % 11)
            if (parity[sum % 11] != value[17]) {
                callback(new Error('校验位错误'));
                return
            } else {
                callback();
                return
            }
        }
    }
}
export const userNameCheck = async (_: any, value: any, callback: any) => {
    try {
        await checkUsername(value.toString())
        callback()
        return
    } catch (e) {
        if (e instanceof MinihError) {
            callback(new Error(e.msg))
            return
        }
        callback(new Error("用户名校验不通过"))
        return
    }

}
export const passwordCheck = async (_: any, value: any, callback: any) => {
    if (!value) {
        callback();
        return
    }
    try {
        await checkPassword(value.toString())
        callback()
        return
    } catch (e) {
        if (e instanceof MinihError) {
            callback(new Error(e.msg))
            return
        }
        callback(new Error("密码校验不通过"))
        return
    }

}
export const isNil = (v: unknown): boolean => {
    return typeof v === "undefined" || v === null
}
export const objectIsEqual = (o1: Record<string, any>, o2: Record<string, any>): boolean => {
    if (Object.keys(o1).length !== Object.keys(o2).length) {
        return false // 首先对象属性个数要一致，不一致的话其内容也就不需要判断了
    } else {
        return Object.keys(o1).every((k) => {
            if (Object.keys(o2).includes(k)) {
                return anyIsEqual(o1[k], o2[k])
            } else {
                return false
            } // 只要其中一个对象的属性名在另一个对象存在，就判断其值是否相等就可以了
        })
    }
}
export const anyIsEqual = (a1: any, a2: any, ignoreArrayPosition = false): boolean => {
    if (isNil(a1) || isNil(a2)) { // 首先判断要比较的两个参数是否为空
        return a1 === a2 // 只要其中一个为空的话，直接返回它们是否相等就可以了
    } else {
        if (a1.constructor === a2.constructor) { // 再判断他们的类型是否相等，类型不相等的话，其值也无须判断了
            if (a1.constructor === Array) {
                return arrayIsEqual(a1, a2, ignoreArrayPosition) // 如果是数组，就用数组方式判断是否相等
            } else if (a1.constructor === Object) {
                return objectIsEqual(a1, a2) // 如果是对象，就用对象方式判断是否相等
            } else {
                return a1 === a2 // 其余的直接判断是否相等
            }
        } else {
            return false
        }
    }
}
export const arrayIsEqual = (a1: Array<any>, a2: Array<any>, ignoreArrayPosition = false): boolean => {
    if (a1.length !== a2.length) {
        return false // 判断数组大小是否一致，不一致的话其内容也就不需要判断了
    } else {
        if (!ignoreArrayPosition) {
            return a1.every((v, i) => anyIsEqual(v, a2[i])) // 如果不忽略数组位置的话，也就是说每个数组元素的位置一样，并且其值也一样才判断为相等
        } else {
            return a1.every(
                (v) =>
                    a2.findIndex((v2) => anyIsEqual(v2, v)) >= 0 &&
                    a2.filter((v2) => anyIsEqual(v2, v)).length === a1.filter((v1) => anyIsEqual(v1, v)).length,
            ) // 忽略数据位置的话，那么只要其中一个数组每个元素都可以在另一个数组可以找到，并且每个元素在两个数组的数量是相等的话，那么就可以判定为相等
        }
    }
}