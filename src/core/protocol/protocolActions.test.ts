import { parseInput as realParseInput } from './protocolActions';

describe('ProtocolActions validator', () => {
  const parseInput = (input: string) => realParseInput(input, false);

  test('Incorrect protocol should fail', () => {
    const input = 'ftb-wrong:/modpack/install';
    const result = parseInput(input);

    expect(result).toBeNull();
  });

  test('Simple action should resolve', () => {
    const input = 'ftb-dev:/modpack/install';
    const result = parseInput(input);

    expect(result?.namespace && result.action).toBeTruthy();
  });

  test('Simple action with double slash should resolve', () => {
    const input = 'ftb-dev://modpack/install';
    const result = parseInput(input);

    expect(result?.namespace && result.action).toBeTruthy();
  });

  test('Invalid simple action should fail', () => {
    const input = 'ftb-dev:/modpackinstall';
    const result = parseInput(input);

    expect(result).toBeNull();
  });

  test('Action with args should resolve', () => {
    const input = 'ftb-dev:/modpack/install/test';
    const result = parseInput(input);

    expect(result?.args).toContain('test');
  });

  test('Action without args should not have args', () => {
    const input = 'ftb-dev:/modpack/install';
    const result = parseInput(input);

    expect(result?.args).toEqual([]);
  });

  test('Action with with query should resolve', () => {
    const input = 'ftb-dev:/modpack/install/test?help=true';
    const result = parseInput(input);

    expect(result?.query.has('help')).toBeTruthy();
    expect(result?.query.get('help')).toEqual('true');
  });

  test('Action with with invalid query should fail', () => {
    const input = 'ftb-dev:/modpack/install/test£help=true';
    const result = parseInput(input);

    expect(result?.query.has('help')).toBeFalsy();
  });
});
