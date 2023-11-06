import {ModpackInstallAction} from './actions/ModpackInstallAction';
import {AuthAction} from '@/core/protocol/actions/AuthAction';
import {consoleBadButNoLogger} from '@/utils';

export type ActionType = 'modpack' | 'auth';

export type ActionContext = {
  self: Action;
  args: string[];
  raw: string;
  query: URLSearchParams;
};

export interface Action {
  namespace: ActionType;
  action: string;
  run: (context: ActionContext) => void;
}

const protocolSpace = 'ftb';

const actions: Action[] = [
  // NOTE: Used in test cases, don't remove without updating tests!
  new ModpackInstallAction(),
  new AuthAction(),
];

export const parseInput = (rawInput: string, log = true) => {
  if (rawInput == null) {
    return null;
  }

  const lowerRawInput = rawInput.toLowerCase(); // Mutates argument

  if (!lowerRawInput.startsWith(`${protocolSpace}:/`)) {
    return null;
  }

  // Remove the protocol from the input with optional support for :// or :/
  const seperatedInput = rawInput.slice(protocolSpace.length + (rawInput.includes('://') ? 3 : 2));
  const inputParts = seperatedInput.split('/');

  if (inputParts.length < 2) {
    if (log) {
      consoleBadButNoLogger("W", 'Unable to find at least a namespace and action from the url');
    }

    return null;
  }

  const lastPart = inputParts[inputParts.length - 1];
  return {
    namespace: inputParts[0],
    action: inputParts[1].split('?')[0],
    args: inputParts.slice(2), // Everything else
    query: new URLSearchParams(lastPart.split('?')[lastPart.split('?').length - 1]),
  };
};

export const handleAction = (rawInput: string) => {
  const input = parseInput(rawInput);
  if (input == null) {
    consoleBadButNoLogger("W", 'Rejecting protocol as we could not parse the input', rawInput);
    return;
  }

  const runner = actions.find((e) => e.namespace === input.namespace && e.action === input.action);
  if (runner) {
    runner.run({
      self: runner,
      args: input.args,
      query: input.query,
      raw: rawInput,
    });
  }
};
