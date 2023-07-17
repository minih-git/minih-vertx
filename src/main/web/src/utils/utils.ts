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
