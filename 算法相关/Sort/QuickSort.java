import java.util.Arrays;

public class QuickSort {
  public static void main(String[] args) {
    int[] arr = {2019, 4, 1, 2, 40, 54, 117000};
    quickSort(arr, 0, arr.length - 1);
    System.out.println(Arrays.toString(arr));
  }

  private static void quickSort(int arr[], int left, int right) {
    if (left < right) {
      int pivot = partition(arr, left, right);
      quickSort(arr, left, pivot - 1);
      quickSort(arr, pivot + 1, right);
    }
  }

  private static int partition(int arr[], int left, int right) {
    int pivot = arr[right];
    while (left < right) {
      while (left < right && arr[left] < pivot) left++;
      while (left < right && arr[right] > pivot) right--;

      int temp = arr[left];
      arr[left] = arr[right];
      arr[right] = temp;
    }
    
    return left;
  }
}
