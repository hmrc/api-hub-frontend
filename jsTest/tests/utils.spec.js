import {pluralise} from "../../app/assets/javascripts/utils.js";

describe('utils', () => {
    describe('pluralise', () => {
        const text = "result";
        it("correctly pluralises text when count is 0", () => {
            expect(pluralise(text, 0)).toBe(text + "s");
        });

        it("correctly pluralises text when count is 1", () => {
            expect(pluralise(text, 1)).toBe(text);
        });

        it("correctly pluralises text when count is '1'", () => {
            expect(pluralise(text, '1')).toBe(text);
        });

        it("correctly pluralises text when count is > 1", () => {
            expect(pluralise(text, 123)).toBe(text + "s");
        });

        it("correctly pluralises text when count is > 1 and is a string", () => {
            expect(pluralise(text, '123')).toBe(text + "s");
        });

        it("correctly pluralises text with custom suffix", () => {
            expect(pluralise("loss", 2, "es")).toBe("losses");
        });
    });
})
