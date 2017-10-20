import {color, rgb} from 'd3-color';
import {scaleLinear} from "d3-scale";
import {range} from 'd3-array';

export class ColorMap {
    private colorSteps;
    private forwardArray = [];
    private inverseMap = {};

    constructor(config) {

        let scale = scaleLinear().domain(range(0, 256, 256 / config.steps.length)).range(config.steps);

        for (let i=0; i < 256; ++i) {
            let c = rgb(scale(i));
            c = [c.r, c.g, c.b, c.opacity * 255];
            this.forwardArray.push(c);
            this.inverseMap[ColorMap.rgbToHex(c)] = i;
        }

        this.colorSteps = config.steps;
    }

    getColorSteps() {
        return this.colorSteps;
    }

    getForwardArray() {
        return this.forwardArray;
    }

    getInverseMap() {
        return this.inverseMap;
    }

    forward(value) {
        return this.forwardArray[value];
    }

    inverse(hex) {
        return this.inverseMap[hex];
    }


    static rgbToHex(c) {
        return "#" + ((1 << 24) + (c[0] << 16) + (c[1] << 8) + c[2]).toString(16).slice(1);
    }

}