import request from '@/utils/request.js';

const CHILD_PATH = '/backend';
export const getReadFile = () => {
  return request.get(CHILD_PATH + '/readFile');
}