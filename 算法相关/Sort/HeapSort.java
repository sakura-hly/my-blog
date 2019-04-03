import java.util.Arrays;

public class HeapSort {
  public static void main(String[] args) {
    int[] arr = {2019, 4, 1, 2, 40, 54, 117000};
    heapSort(arr);
    System.out.println(Arrays.toString(arr));
  }

  private static void heapSort(int arr[]) {
    for (int i = (arr.length - 1) >> 1; i >= 0; i--) {
      adjustHeap(arr, i, arr.length);
    }

    for (int i = arr.length - 1; i >= 0; i--) {
      int temp = arr[0];
      arr[0] = arr[i];
      arr[i] = temp;
      adjustHeap(arr, 0, i);
    }
  }

  private static void adjustHeap(int arr[], int i, int len) {
    int temp = arr[i];
    while ((i << 1) + 1 < len) {
      int child = (i << 1) + 1;
      if (child + 1 < len && arr[child] < arr[child + 1]) {
        child++;
      }
      if (arr[child] > temp) {
        arr[i] = arr[child];
        i = child;
      } else {
        break;
      }
    }
    arr[i] = temp;
  }
}
